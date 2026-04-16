package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.enums.H5Locale;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.util.H5LocaleContext;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class H5I18nServiceImpl implements H5I18nService {

    private static final Map<H5Locale, Map<String, String>> MESSAGES = Map.of(
            H5Locale.ZH_TW, traditionalChineseMessages(),
            H5Locale.EN_US, englishMessages(),
            H5Locale.VI_VN, vietnameseMessages()
    );

    @Override
    public H5Locale currentLocale() {
        return H5LocaleContext.current();
    }

    @Override
    public String text(String key, String fallback) {
        return MESSAGES.getOrDefault(currentLocale(), Map.of()).getOrDefault(key, fallback);
    }

    private static Map<String, String> traditionalChineseMessages() {
        return Map.ofEntries(
                Map.entry("loan.term.1", "1期"),
                Map.entry("loan.term.3", "3期"),
                Map.entry("loan.term.6", "6期"),
                Map.entry("loan.lender", "XX商業銀行"),
                Map.entry("loan.receivingAccount.bankName", "招商銀行"),
                Map.entry("loan.approval.submit.name", "提交申請"),
                Map.entry("loan.approval.submit.description", "申請已提交成功"),
                Map.entry("loan.approval.reviewing.name", "審批中"),
                Map.entry("loan.approval.reviewing.description", "正在進行資質審核..."),
                Map.entry("loan.approval.waiting.name", "等待放款"),
                Map.entry("loan.approval.waiting.description", "審批通過後即可放款"),
                Map.entry("loan.approval.approved.name", "審批完成"),
                Map.entry("loan.approval.approved.description", "資質審核已通過"),
                Map.entry("loan.approval.disburse.name", "準備放款"),
                Map.entry("loan.approval.disburse.description", "資金將在30分鐘內到帳"),
                Map.entry("loan.approval.rejected.name", "審批完成"),
                Map.entry("loan.approval.rejected.description", "暫未通過本次審核"),
                Map.entry("loan.approval.rejected.disburse.name", "準備放款"),
                Map.entry("loan.approval.rejected.disburse.description", "審核未通過，無法放款"),
                Map.entry("loan.approval.arrivalTime", "30分鐘"),
                Map.entry("loan.approval.result.tip.approved", "審批通過，預計30分鐘內到帳"),
                Map.entry("loan.approval.tip.approved", "您已成功開通惠選卡，放款完成後，您可自主領取並生效對應的權益。"),
                Map.entry("loan.approval.tip.rejected", "借款申請未通過，但權益已購買成功。"),
                Map.entry("loan.apply.failurePrefix", "權益購買成功，借款申請失敗："),
                Map.entry("benefits.cardName", "惠選卡"),
                Map.entry("benefits.feature.0.title", "先享後付，無壓力消費"),
                Map.entry("benefits.feature.0.description", "支持多種消費場景，權益開通後可按規則使用。"),
                Map.entry("benefits.feature.1.title", "費用預估¥100/月，共3期"),
                Map.entry("benefits.feature.1.description", "當前按 3 期固定展示，便於前端與後端先聯通。"),
                Map.entry("benefits.feature.2.title", "匹配優質權益，不成功不收費"),
                Map.entry("benefits.feature.2.description", "結合當前權益服務能力返回統一展示信息。"),
                Map.entry("benefits.category.tv.name", "影音會員"),
                Map.entry("benefits.category.car.name", "出行服務"),
                Map.entry("benefits.item.tv.0.title", "每月4選1影視VIP會員"),
                Map.entry("benefits.item.tv.0.description", "騰訊視頻、優酷、愛奇藝、芒果TV任選"),
                Map.entry("benefits.item.tv.0.validity", "有效期30天/次"),
                Map.entry("benefits.item.car.0.title", "出行禮包"),
                Map.entry("benefits.item.car.0.description", "覆蓋打車和代駕等高頻場景"),
                Map.entry("benefits.item.car.0.validity", "有效期30天/次"),
                Map.entry("benefits.tip.0", "權益服務費基於惠聚會員服務收取，請以頁面展示說明為準。"),
                Map.entry("benefits.tip.1", "本費用為互聯網增值服務費用，成功開通後不支持無理由撤銷。"),
                Map.entry("benefits.tip.2", "您充分知曉服務內容後再進行開通操作。"),
                Map.entry("benefits.protocol.0.name", "用戶服務協議"),
                Map.entry("benefits.protocol.1.name", "隱私條款聲明"),
                Map.entry("benefits.protocol.2.name", "委託扣款協議"),
                Map.entry("benefits.protocol.3.name", "權益服務協議"),
                Map.entry("benefits.activate.success", "惠選卡開通成功"),
                Map.entry("repayment.type.early", "提前還款"),
                Map.entry("repayment.tip.info", "還款後將立即生效，剩餘期數對應的利息將不再收取。請確認銀行卡餘額充足。"),
                Map.entry("repayment.tip.0", "還款金額已從您的銀行卡扣除，請注意查收銀行通知"),
                Map.entry("repayment.tip.1", "提前還款後，您的信用額度將即時恢復"),
                Map.entry("repayment.tip.2", "如需查看還款記錄，可前往\"我的\"-\"帳單明細\""),
                Map.entry("repayment.tip.3", "若有任何疑問，請聯繫客服：400-888-8888")
        );
    }

    private static Map<String, String> englishMessages() {
        return Map.ofEntries(
                Map.entry("loan.term.1", "1 term"),
                Map.entry("loan.term.3", "3 terms"),
                Map.entry("loan.term.6", "6 terms"),
                Map.entry("loan.lender", "XX Commercial Bank"),
                Map.entry("loan.receivingAccount.bankName", "China Merchants Bank"),
                Map.entry("loan.approval.submit.name", "Application Submitted"),
                Map.entry("loan.approval.submit.description", "Your application has been submitted successfully."),
                Map.entry("loan.approval.reviewing.name", "Under Review"),
                Map.entry("loan.approval.reviewing.description", "Qualification review is in progress..."),
                Map.entry("loan.approval.waiting.name", "Awaiting Disbursement"),
                Map.entry("loan.approval.waiting.description", "Funds will be disbursed after approval."),
                Map.entry("loan.approval.approved.name", "Approval Completed"),
                Map.entry("loan.approval.approved.description", "Your qualification review has been approved."),
                Map.entry("loan.approval.disburse.name", "Preparing Disbursement"),
                Map.entry("loan.approval.disburse.description", "Funds will arrive within 30 minutes."),
                Map.entry("loan.approval.rejected.name", "Approval Completed"),
                Map.entry("loan.approval.rejected.description", "This application was not approved."),
                Map.entry("loan.approval.rejected.disburse.name", "Preparing Disbursement"),
                Map.entry("loan.approval.rejected.disburse.description", "Disbursement is unavailable because the review was not approved."),
                Map.entry("loan.approval.arrivalTime", "30 minutes"),
                Map.entry("loan.approval.result.tip.approved", "Approved. Funds are expected to arrive within 30 minutes."),
                Map.entry("loan.approval.tip.approved", "You have successfully activated the Benefit Card. After disbursement, you can claim and activate the corresponding benefits."),
                Map.entry("loan.approval.tip.rejected", "The loan application was not approved, but the benefit purchase succeeded."),
                Map.entry("loan.apply.failurePrefix", "Benefit purchase succeeded, but the loan application failed: "),
                Map.entry("benefits.cardName", "Benefit Card"),
                Map.entry("benefits.feature.0.title", "Enjoy first, pay later"),
                Map.entry("benefits.feature.0.description", "Supports multiple usage scenarios. Activate benefits first and pay according to the rules later."),
                Map.entry("benefits.feature.1.title", "Estimated fee ¥100/month for 3 months"),
                Map.entry("benefits.feature.1.description", "Currently displayed with a fixed 3-term plan to simplify H5-backend integration."),
                Map.entry("benefits.feature.2.title", "Matched premium benefits, no fee if unsuccessful"),
                Map.entry("benefits.feature.2.description", "Returns unified display content based on the current benefits service capability."),
                Map.entry("benefits.category.tv.name", "Streaming Membership"),
                Map.entry("benefits.category.car.name", "Travel Service"),
                Map.entry("benefits.item.tv.0.title", "Choose 1 of 4 streaming VIP passes each month"),
                Map.entry("benefits.item.tv.0.description", "Choose from Tencent Video, Youku, iQIYI, or Mango TV"),
                Map.entry("benefits.item.tv.0.validity", "Valid for 30 days each time"),
                Map.entry("benefits.item.car.0.title", "Travel package"),
                Map.entry("benefits.item.car.0.description", "Covers frequent scenarios such as ride-hailing and chauffeur service"),
                Map.entry("benefits.item.car.0.validity", "Valid for 30 days each time"),
                Map.entry("benefits.tip.0", "The benefits service fee is charged based on the HuiJu membership service. Please refer to the page display."),
                Map.entry("benefits.tip.1", "This fee is an internet value-added service fee and cannot be revoked without reason after successful activation."),
                Map.entry("benefits.tip.2", "Please activate only after fully understanding the service content."),
                Map.entry("benefits.protocol.0.name", "User Service Agreement"),
                Map.entry("benefits.protocol.1.name", "Privacy Statement"),
                Map.entry("benefits.protocol.2.name", "Debit Authorization Agreement"),
                Map.entry("benefits.protocol.3.name", "Benefits Service Agreement"),
                Map.entry("benefits.activate.success", "Benefit Card activated successfully"),
                Map.entry("repayment.type.early", "Early repayment"),
                Map.entry("repayment.tip.info", "Repayment takes effect immediately, and interest for the remaining terms will no longer be charged. Please make sure your bank card has sufficient balance."),
                Map.entry("repayment.tip.0", "The repayment amount has been deducted from your bank card. Please check your bank notification."),
                Map.entry("repayment.tip.1", "Your credit limit will recover immediately after early repayment."),
                Map.entry("repayment.tip.2", "To view repayment records, go to My Account - Bill Details."),
                Map.entry("repayment.tip.3", "If you have any questions, please contact customer service: 400-888-8888.")
        );
    }

    private static Map<String, String> vietnameseMessages() {
        return Map.ofEntries(
                Map.entry("loan.term.1", "1 kỳ"),
                Map.entry("loan.term.3", "3 kỳ"),
                Map.entry("loan.term.6", "6 kỳ"),
                Map.entry("loan.lender", "Ngân hàng Thương mại XX"),
                Map.entry("loan.receivingAccount.bankName", "Ngân hàng China Merchants"),
                Map.entry("loan.approval.submit.name", "Đã gửi hồ sơ"),
                Map.entry("loan.approval.submit.description", "Hồ sơ vay đã được gửi thành công."),
                Map.entry("loan.approval.reviewing.name", "Đang thẩm định"),
                Map.entry("loan.approval.reviewing.description", "Đang tiến hành đánh giá điều kiện..."),
                Map.entry("loan.approval.waiting.name", "Chờ giải ngân"),
                Map.entry("loan.approval.waiting.description", "Sẽ giải ngân sau khi được phê duyệt."),
                Map.entry("loan.approval.approved.name", "Đã hoàn tất phê duyệt"),
                Map.entry("loan.approval.approved.description", "Hồ sơ của bạn đã được phê duyệt."),
                Map.entry("loan.approval.disburse.name", "Chuẩn bị giải ngân"),
                Map.entry("loan.approval.disburse.description", "Tiền sẽ đến trong vòng 30 phút."),
                Map.entry("loan.approval.rejected.name", "Đã hoàn tất phê duyệt"),
                Map.entry("loan.approval.rejected.description", "Hồ sơ lần này chưa được phê duyệt."),
                Map.entry("loan.approval.rejected.disburse.name", "Chuẩn bị giải ngân"),
                Map.entry("loan.approval.rejected.disburse.description", "Không thể giải ngân do hồ sơ không được duyệt."),
                Map.entry("loan.approval.arrivalTime", "30 phút"),
                Map.entry("loan.approval.result.tip.approved", "Đã phê duyệt, dự kiến tiền sẽ đến trong vòng 30 phút."),
                Map.entry("loan.approval.tip.approved", "Bạn đã kích hoạt thành công Thẻ Quyền Lợi. Sau khi giải ngân, bạn có thể nhận và kích hoạt quyền lợi tương ứng."),
                Map.entry("loan.approval.tip.rejected", "Yêu cầu vay không được duyệt nhưng giao dịch mua quyền lợi đã thành công."),
                Map.entry("loan.apply.failurePrefix", "Mua quyền lợi thành công nhưng yêu cầu vay thất bại: "),
                Map.entry("benefits.cardName", "Thẻ Quyền Lợi"),
                Map.entry("benefits.feature.0.title", "Dùng trước, trả sau"),
                Map.entry("benefits.feature.0.description", "Hỗ trợ nhiều tình huống sử dụng, kích hoạt quyền lợi trước rồi thanh toán theo quy định."),
                Map.entry("benefits.feature.1.title", "Ước tính phí ¥100/tháng trong 3 kỳ"),
                Map.entry("benefits.feature.1.description", "Hiện hiển thị cố định 3 kỳ để H5 và backend dễ tích hợp trước."),
                Map.entry("benefits.feature.2.title", "Ghép quyền lợi chất lượng, không thành công không mất phí"),
                Map.entry("benefits.feature.2.description", "Trả về nội dung hiển thị thống nhất theo năng lực dịch vụ quyền lợi hiện tại."),
                Map.entry("benefits.category.tv.name", "Hội viên giải trí"),
                Map.entry("benefits.category.car.name", "Dịch vụ di chuyển"),
                Map.entry("benefits.item.tv.0.title", "Mỗi tháng chọn 1 trong 4 gói VIP xem phim"),
                Map.entry("benefits.item.tv.0.description", "Chọn từ Tencent Video, Youku, iQIYI hoặc Mango TV"),
                Map.entry("benefits.item.tv.0.validity", "Hiệu lực 30 ngày/lần"),
                Map.entry("benefits.item.car.0.title", "Gói ưu đãi di chuyển"),
                Map.entry("benefits.item.car.0.description", "Bao phủ các nhu cầu thường dùng như gọi xe và lái hộ"),
                Map.entry("benefits.item.car.0.validity", "Hiệu lực 30 ngày/lần"),
                Map.entry("benefits.tip.0", "Phí dịch vụ quyền lợi được thu theo dịch vụ hội viên HuiJu, vui lòng tham khảo thông tin hiển thị trên trang."),
                Map.entry("benefits.tip.1", "Đây là phí dịch vụ giá trị gia tăng trên internet, sau khi kích hoạt thành công sẽ không hỗ trợ hủy vô lý do."),
                Map.entry("benefits.tip.2", "Vui lòng chỉ kích hoạt sau khi đã hiểu đầy đủ nội dung dịch vụ."),
                Map.entry("benefits.protocol.0.name", "Thỏa thuận dịch vụ người dùng"),
                Map.entry("benefits.protocol.1.name", "Tuyên bố quyền riêng tư"),
                Map.entry("benefits.protocol.2.name", "Thỏa thuận ủy quyền khấu trừ"),
                Map.entry("benefits.protocol.3.name", "Thỏa thuận dịch vụ quyền lợi"),
                Map.entry("benefits.activate.success", "Kích hoạt Thẻ Quyền Lợi thành công"),
                Map.entry("repayment.type.early", "Trả trước hạn"),
                Map.entry("repayment.tip.info", "Khoản trả nợ sẽ có hiệu lực ngay lập tức và lãi cho các kỳ còn lại sẽ không bị thu nữa. Vui lòng đảm bảo số dư thẻ ngân hàng đủ."),
                Map.entry("repayment.tip.0", "Khoản tiền trả nợ đã được khấu trừ từ thẻ ngân hàng của bạn, vui lòng kiểm tra thông báo ngân hàng."),
                Map.entry("repayment.tip.1", "Hạn mức tín dụng sẽ được khôi phục ngay sau khi trả trước hạn."),
                Map.entry("repayment.tip.2", "Để xem lịch sử trả nợ, vui lòng vào Tài khoản của tôi - Chi tiết hóa đơn."),
                Map.entry("repayment.tip.3", "Nếu có thắc mắc, vui lòng liên hệ CSKH: 400-888-8888")
        );
    }
}
