package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.dto.request.JointLoginRequest;
import com.nexusfin.equity.dto.request.RepaymentLoginRequest;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.JointLoginService;
import com.nexusfin.equity.service.RepaymentLoginService;
import com.nexusfin.equity.util.TraceIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepaymentLoginServiceImpl implements RepaymentLoginService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentLoginServiceImpl.class);

    private final JointLoginService jointLoginService;
    private final MemberInfoRepository memberInfoRepository;
    private final LoanApplicationMappingRepository loanApplicationMappingRepository;

    public RepaymentLoginServiceImpl(
            JointLoginService jointLoginService,
            MemberInfoRepository memberInfoRepository,
            LoanApplicationMappingRepository loanApplicationMappingRepository
    ) {
        this.jointLoginService = jointLoginService;
        this.memberInfoRepository = memberInfoRepository;
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
    }

    @Override
    @Transactional
    public RepaymentLoginResult login(RepaymentLoginRequest request) {
        JointLoginService.JointLoginResult jointLoginResult = loginWithRepaymentToken(request);
        MemberInfo memberInfo = resolveMemberInfo(jointLoginResult.externalUserId());
        validateLoanOwnership(memberInfo.getMemberId(), request.loanId());
        log.info("traceId={} bizOrderNo={} memberId={} externalUserId={} tokenPresent={} repayment login succeeded",
                TraceIdUtil.getTraceId(),
                request.loanId(),
                memberInfo.getMemberId(),
                jointLoginResult.externalUserId(),
                true);
        return new RepaymentLoginResult(jointLoginResult.jwtToken(), request.loanId());
    }

    private JointLoginService.JointLoginResult loginWithRepaymentToken(RepaymentLoginRequest request) {
        try {
            return jointLoginService.login(new JointLoginRequest(
                    request.token(),
                    "push",
                    null,
                    null,
                    null
            ));
        } catch (BizException exception) {
            if ("JOINT_LOGIN_TOKEN_INVALID".equals(exception.getErrorNo())) {
                throw new BizException("REPAYMENT_LOGIN_TOKEN_INVALID", "Repayment login session expired");
            }
            throw exception;
        }
    }

    private MemberInfo resolveMemberInfo(String externalUserId) {
        MemberInfo memberInfo = memberInfoRepository.selectByTechPlatformUserId(externalUserId);
        if (memberInfo == null) {
            memberInfo = memberInfoRepository.selectByCid(externalUserId);
        }
        if (memberInfo == null) {
            throw new BizException("REPAYMENT_LOGIN_MEMBER_NOT_FOUND", "Repayment login member not found");
        }
        return memberInfo;
    }

    private void validateLoanOwnership(String memberId, Integer loanId) {
        LoanApplicationMapping mapping = loanApplicationMappingRepository.selectOne(
                Wrappers.<LoanApplicationMapping>lambdaQuery()
                        .eq(LoanApplicationMapping::getMemberId, memberId)
                        .eq(LoanApplicationMapping::getPlatformLoanId, loanId)
                        .last("limit 1")
        );
        if (mapping == null) {
            log.warn("traceId={} bizOrderNo={} memberId={} errorNo={} errorMsg={} repayment login loan ownership rejected",
                    TraceIdUtil.getTraceId(),
                    loanId,
                    memberId,
                    "REPAYMENT_LOGIN_LOAN_FORBIDDEN",
                    "Repayment loan does not belong to current user");
            throw new BizException("REPAYMENT_LOGIN_LOAN_FORBIDDEN", "Repayment loan does not belong to current user");
        }
    }
}
