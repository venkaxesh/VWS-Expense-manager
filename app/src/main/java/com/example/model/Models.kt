package com.example.model

enum class UserRole {
    ADMIN,
    MEMBER
}

enum class MemberStatus {
    ACTIVE,
    PENDING_APPROVAL,
    SUSPENDED
}

enum class ExpenseCategory(val displayName: String) {
    ELECTRICITY_EB("Electricity (EB)"),
    ESSENTIALS("Essentials"),
    WATER("Water"),
    ROOM_RENT("Room Rent"),
    INTERNET("Internet"),
    MAINTENANCE("Maintenance"),
    OTHER("Other")
}

enum class SplitType(val displayName: String) {
    EQUAL("Equal Split"),
    RATIO("Ratio Split"),
    SEPARATE_AMOUNT("Separate Amount"),
    PERCENTAGE("Percentage Split")
}

enum class PaymentStatus(val displayName: String) {
    PENDING("Pending"),
    PAYMENT_SUBMITTED("Payment Submitted"),
    PAID("Paid"),
    DISPUTED("Disputed")
}

enum class PaymentMethod(val displayName: String) {
    UPI("UPI"),
    CASH("Cash"),
    BANK_TRANSFER("Bank Transfer")
}

enum class NoteType(val displayName: String) {
    SHARED_ROOM("Shared Room Note"),
    PRIVATE("Private Note"),
    PAYMENT_REMINDER("Payment Reminder"),
    DUE_DATE("Due Date Alert"),
    ADMIN_ANNOUNCEMENT("Admin Notice")
}

data class User(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String = "",
    val upiId: String = "",
    val bankAccountNumber: String = "",
    val ifscCode: String = "",
    val role: UserRole = UserRole.MEMBER,
    val isApproved: Boolean = true
) {
    val sixDigitUserId: String
        get() {
            val digits = id.filter { it.isDigit() }
            return if (digits.length >= 6) {
                digits.takeLast(6)
            } else {
                val num = (Math.abs(id.hashCode()) % 900000) + 100000
                num.toString()
            }
        }

    val shareableUserId: String
        get() = sixDigitUserId

    val maskedAccountNumber: String
        get() = if (bankAccountNumber.length > 4) {
            "XXXX-XXXX-${bankAccountNumber.takeLast(4)}"
        } else "XXXX"
}

data class Room(
    val id: String,
    val name: String,
    val address: String,
    val ebConsumerNumber: String,
    val waterTaxNumber: String,
    val ownerName: String,
    val ownerPhone: String,
    val monthlyRentPaise: Long,
    val rentDueDay: Int,
    val description: String,
    val adminUid: String,
    val invitationCode: String = "",
    val isArchived: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val memberUids: List<String> get() = emptyList()
    val monthlyRentAmount: Long get() = monthlyRentPaise
    val ebServiceNumber: String get() = ebConsumerNumber
    val waterConnectionNumber: String get() = waterTaxNumber
}

data class RoomMember(
    val id: String,
    val roomId: String,
    val userId: String,
    val role: UserRole,
    val status: MemberStatus,
    val joinedAtMillis: Long = System.currentTimeMillis()
)

data class Expense(
    val id: String,
    val roomId: String,
    val createdByUserId: String,
    val payerUserId: String,
    val description: String,
    val category: ExpenseCategory,
    val customCategoryName: String? = null,
    val totalAmountPaise: Long,
    val expenseDateMillis: Long,
    val splitType: SplitType,
    val billReceiptUri: String? = null,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class ExpenseSplit(
    val id: String,
    val expenseId: String,
    val roomId: String,
    val memberUserId: String,
    val allocatedAmountPaise: Long,
    val splitValueInput: String = "",
    val isPaid: Boolean = false
) {
    val isSettled: Boolean get() = isPaid
}

data class PaymentRecord(
    val id: String,
    val roomId: String,
    val fromUserId: String,
    val toUserId: String,
    val amountPaise: Long,
    val paymentMethod: PaymentMethod,
    val referenceId: String = "",
    val status: PaymentStatus,
    val relatedExpenseId: String? = null,
    val submittedAtMillis: Long = System.currentTimeMillis(),
    val verifiedAtMillis: Long? = null,
    val verifiedByUserId: String? = null,
    val proofNotes: String = ""
)

data class RoomNote(
    val id: String,
    val roomId: String,
    val authorUserId: String,
    val title: String,
    val content: String,
    val noteType: NoteType,
    val isPrivate: Boolean = false,
    val targetUserId: String? = null,
    val isRead: Boolean = false,
    val dueDateMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class AuditLog(
    val id: String,
    val roomId: String,
    val performedByUserId: String,
    val actionType: String,
    val description: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val previousState: String? = null,
    val newState: String? = null
)

data class SettlementSuggestion(
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amountPaise: Long,
    val toUserUpiId: String
)

object CurrencyHelper {
    fun formatPaise(paise: Long): String = formatPaiseToRupee(paise)

    fun formatPaiseToRupee(paise: Long): String {
        val isNegative = paise < 0
        val absPaise = kotlin.math.abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100
        val formatted = if (remainingPaise == 0L) {
            String.format("%,d", rupees)
        } else {
            String.format("%,d.%02d", rupees, remainingPaise)
        }
        return if (isNegative) "-₹$formatted" else "₹$formatted"
    }

    fun rupeeToPaise(rupeeStr: String): Long {
        val clean = rupeeStr.trim().replace(",", "").replace("₹", "")
        if (clean.isBlank()) return 0L
        val parts = clean.split(".")
        val rupees = parts[0].toLongOrNull() ?: 0L
        val paise = if (parts.size > 1) {
            val decimals = parts[1].take(2).padEnd(2, '0')
            decimals.toLongOrNull() ?: 0L
        } else {
            0L
        }
        return rupees * 100 + paise
    }

    fun formatPaiseToPlainRupee(paise: Long): String {
        val rupees = paise / 100
        val remainingPaise = paise % 100
        return if (remainingPaise == 0L) "$rupees" else String.format(java.util.Locale.US, "%d.%02d", rupees, remainingPaise)
    }
}
