package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.dto.response.SignTaskResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.ContractArchive;
import com.nexusfin.equity.entity.SignTask;
import com.nexusfin.equity.enums.SignStatusEnum;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.service.AgreementService;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.SensitiveDataUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgreementServiceImpl implements AgreementService {

    private static final List<String> CONTRACT_TYPES = List.of("EQUITY_AGREEMENT", "DEFERRED_AGREEMENT");

    private final SignTaskRepository signTaskRepository;
    private final ContractArchiveRepository contractArchiveRepository;

    public AgreementServiceImpl(
            SignTaskRepository signTaskRepository,
            ContractArchiveRepository contractArchiveRepository
    ) {
        this.signTaskRepository = signTaskRepository;
        this.contractArchiveRepository = contractArchiveRepository;
    }

    @Override
    @Transactional
    public SignTaskResponse ensureAgreementArtifacts(BenefitOrder order) {
        // 基线阶段先把两类协议任务和合同归档都沉淀下来，后续如果改成真实电子签章平台，只需要替换这里的生成逻辑。
        List<SignTaskResponse.SignTaskItem> taskItems = new ArrayList<>();
        for (String contractType : CONTRACT_TYPES) {
            SignTask signTask = signTaskRepository.selectOne(Wrappers.<SignTask>lambdaQuery()
                    .eq(SignTask::getBenefitOrderNo, order.getBenefitOrderNo())
                    .eq(SignTask::getContractType, contractType)
                    .last("limit 1"));
            if (signTask == null) {
                signTask = new SignTask();
                signTask.setTaskNo(RequestIdUtil.nextId("tsk"));
                signTask.setBenefitOrderNo(order.getBenefitOrderNo());
                signTask.setContractType(contractType);
                signTask.setSignUrl("https://abs.example.com/sign/" + order.getBenefitOrderNo() + "/" + contractType);
                signTask.setSignStatus(SignStatusEnum.SIGNED.name());
                signTask.setCreatedTs(LocalDateTime.now());
                signTask.setUpdatedTs(LocalDateTime.now());
                signTaskRepository.insert(signTask);
            }
            ContractArchive archive = contractArchiveRepository.selectOne(Wrappers.<ContractArchive>lambdaQuery()
                    .eq(ContractArchive::getTaskNo, signTask.getTaskNo())
                    .last("limit 1"));
            if (archive == null) {
                // 归档记录保存合同访问地址和摘要，满足“事后可证明”的追溯要求。
                ContractArchive contractArchive = new ContractArchive();
                contractArchive.setContractNo(RequestIdUtil.nextId("ctr"));
                contractArchive.setTaskNo(signTask.getTaskNo());
                contractArchive.setContractType(contractType);
                contractArchive.setFileUrl("https://abs.example.com/contracts/" + signTask.getTaskNo() + ".pdf");
                contractArchive.setFileHash(SensitiveDataUtil.sha256(signTask.getTaskNo()));
                contractArchive.setCreatedTs(LocalDateTime.now());
                contractArchiveRepository.insert(contractArchive);
            }
            taskItems.add(new SignTaskResponse.SignTaskItem(
                    signTask.getTaskNo(),
                    signTask.getContractType(),
                    signTask.getSignStatus(),
                    signTask.getSignUrl()
            ));
        }
        return new SignTaskResponse(order.getBenefitOrderNo(), taskItems);
    }
}
