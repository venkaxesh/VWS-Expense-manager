package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.model.*

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val upiId: String,
    val bankAccountNumber: String,
    val ifscCode: String,
    val role: String,
    val isApproved: Boolean = true
) {
    fun toDomain() = User(
        id = id,
        username = username,
        fullName = fullName,
        email = email,
        phone = phone,
        upiId = upiId,
        bankAccountNumber = bankAccountNumber,
        ifscCode = ifscCode,
        role = UserRole.valueOf(role),
        isApproved = isApproved
    )

    companion object {
        fun fromDomain(u: User) = UserEntity(
            id = u.id,
            username = u.username,
            fullName = u.fullName,
            email = u.email,
            phone = u.phone,
            upiId = u.upiId,
            bankAccountNumber = u.bankAccountNumber,
            ifscCode = u.ifscCode,
            role = u.role.name,
            isApproved = u.isApproved
        )
    }
}

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
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
    fun toDomain() = Room(
        id = id,
        name = name,
        address = address,
        ebConsumerNumber = ebConsumerNumber,
        waterTaxNumber = waterTaxNumber,
        ownerName = ownerName,
        ownerPhone = ownerPhone,
        monthlyRentPaise = monthlyRentPaise,
        rentDueDay = rentDueDay,
        description = description,
        adminUid = adminUid,
        invitationCode = invitationCode,
        isArchived = isArchived,
        createdAtMillis = createdAtMillis
    )

    companion object {
        fun fromDomain(r: Room) = RoomEntity(
            id = r.id,
            name = r.name,
            address = r.address,
            ebConsumerNumber = r.ebConsumerNumber,
            waterTaxNumber = r.waterTaxNumber,
            ownerName = r.ownerName,
            ownerPhone = r.ownerPhone,
            monthlyRentPaise = r.monthlyRentPaise,
            rentDueDay = r.rentDueDay,
            description = r.description,
            adminUid = r.adminUid,
            invitationCode = r.invitationCode,
            isArchived = r.isArchived,
            createdAtMillis = r.createdAtMillis
        )
    }
}

@Entity(tableName = "room_members")
data class RoomMemberEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val userId: String,
    val role: String,
    val status: String,
    val joinedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain() = RoomMember(
        id = id,
        roomId = roomId,
        userId = userId,
        role = UserRole.valueOf(role),
        status = MemberStatus.valueOf(status),
        joinedAtMillis = joinedAtMillis
    )

    companion object {
        fun fromDomain(rm: RoomMember) = RoomMemberEntity(
            id = rm.id,
            roomId = rm.roomId,
            userId = rm.userId,
            role = rm.role.name,
            status = rm.status.name,
            joinedAtMillis = rm.joinedAtMillis
        )
    }
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val expenseId: String,
    val roomId: String,
    val description: String,
    val amount: Double,
    val paidByUserId: String,
    val timestamp: Long,
    val category: String = "OTHER",
    val customCategoryName: String? = null,
    val splitType: String = "EQUAL",
    val billReceiptUri: String? = null,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val id: String get() = expenseId
    val payerUserId: String get() = paidByUserId
    val createdByUserId: String get() = paidByUserId
    val totalAmountPaise: Long get() = (amount * 100.0).toLong()
    val expenseDateMillis: Long get() = timestamp

    fun toDomain() = Expense(
        id = expenseId,
        roomId = roomId,
        createdByUserId = paidByUserId,
        payerUserId = paidByUserId,
        description = description,
        category = try { ExpenseCategory.valueOf(category) } catch (e: Exception) { ExpenseCategory.OTHER },
        customCategoryName = customCategoryName,
        totalAmountPaise = (amount * 100.0).toLong(),
        expenseDateMillis = timestamp,
        splitType = try { SplitType.valueOf(splitType) } catch (e: Exception) { SplitType.EQUAL },
        billReceiptUri = billReceiptUri,
        notes = notes,
        createdAtMillis = createdAtMillis
    )

    companion object {
        fun fromDomain(e: Expense) = ExpenseEntity(
            expenseId = e.id,
            roomId = e.roomId,
            description = e.description,
            amount = e.totalAmountPaise / 100.0,
            paidByUserId = e.payerUserId,
            timestamp = e.expenseDateMillis,
            category = e.category.name,
            customCategoryName = e.customCategoryName,
            splitType = e.splitType.name,
            billReceiptUri = e.billReceiptUri,
            notes = e.notes,
            createdAtMillis = e.createdAtMillis
        )
    }
}

@Entity(tableName = "expense_splits")
data class ExpenseSplitEntity(
    @PrimaryKey val id: String,
    val expenseId: String,
    val roomId: String,
    val memberUserId: String,
    val allocatedAmountPaise: Long,
    val splitValueInput: String = "",
    val isPaid: Boolean = false
) {
    fun toDomain() = ExpenseSplit(
        id = id,
        expenseId = expenseId,
        roomId = roomId,
        memberUserId = memberUserId,
        allocatedAmountPaise = allocatedAmountPaise,
        splitValueInput = splitValueInput,
        isPaid = isPaid
    )

    companion object {
        fun fromDomain(s: ExpenseSplit) = ExpenseSplitEntity(
            id = s.id,
            expenseId = s.expenseId,
            roomId = s.roomId,
            memberUserId = s.memberUserId,
            allocatedAmountPaise = s.allocatedAmountPaise,
            splitValueInput = s.splitValueInput,
            isPaid = s.isPaid
        )
    }
}

@Entity(tableName = "payment_records")
data class PaymentRecordEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val fromUserId: String,
    val toUserId: String,
    val amountPaise: Long,
    val paymentMethod: String,
    val referenceId: String = "",
    val status: String,
    val relatedExpenseId: String? = null,
    val submittedAtMillis: Long = System.currentTimeMillis(),
    val verifiedAtMillis: Long? = null,
    val verifiedByUserId: String? = null,
    val proofNotes: String = ""
) {
    fun toDomain() = PaymentRecord(
        id = id,
        roomId = roomId,
        fromUserId = fromUserId,
        toUserId = toUserId,
        amountPaise = amountPaise,
        paymentMethod = PaymentMethod.valueOf(paymentMethod),
        referenceId = referenceId,
        status = PaymentStatus.valueOf(status),
        relatedExpenseId = relatedExpenseId,
        submittedAtMillis = submittedAtMillis,
        verifiedAtMillis = verifiedAtMillis,
        verifiedByUserId = verifiedByUserId,
        proofNotes = proofNotes
    )

    companion object {
        fun fromDomain(p: PaymentRecord) = PaymentRecordEntity(
            id = p.id,
            roomId = p.roomId,
            fromUserId = p.fromUserId,
            toUserId = p.toUserId,
            amountPaise = p.amountPaise,
            paymentMethod = p.paymentMethod.name,
            referenceId = p.referenceId,
            status = p.status.name,
            relatedExpenseId = p.relatedExpenseId,
            submittedAtMillis = p.submittedAtMillis,
            verifiedAtMillis = p.verifiedAtMillis,
            verifiedByUserId = p.verifiedByUserId,
            proofNotes = p.proofNotes
        )
    }
}

@Entity(tableName = "room_notes")
data class RoomNoteEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val authorUserId: String,
    val title: String,
    val content: String,
    val noteType: String,
    val isPrivate: Boolean = false,
    val targetUserId: String? = null,
    val isRead: Boolean = false,
    val dueDateMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain() = RoomNote(
        id = id,
        roomId = roomId,
        authorUserId = authorUserId,
        title = title,
        content = content,
        noteType = NoteType.valueOf(noteType),
        isPrivate = isPrivate,
        targetUserId = targetUserId,
        isRead = isRead,
        dueDateMillis = dueDateMillis,
        createdAtMillis = createdAtMillis
    )

    companion object {
        fun fromDomain(n: RoomNote) = RoomNoteEntity(
            id = n.id,
            roomId = n.roomId,
            authorUserId = n.authorUserId,
            title = n.title,
            content = n.content,
            noteType = n.noteType.name,
            isPrivate = n.isPrivate,
            targetUserId = n.targetUserId,
            isRead = n.isRead,
            dueDateMillis = n.dueDateMillis,
            createdAtMillis = n.createdAtMillis
        )
    }
}

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val performedByUserId: String,
    val actionType: String,
    val description: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val previousState: String? = null,
    val newState: String? = null
) {
    fun toDomain() = AuditLog(
        id = id,
        roomId = roomId,
        performedByUserId = performedByUserId,
        actionType = actionType,
        description = description,
        timestampMillis = timestampMillis,
        previousState = previousState,
        newState = newState
    )

    companion object {
        fun fromDomain(a: AuditLog) = AuditLogEntity(
            id = a.id,
            roomId = a.roomId,
            performedByUserId = a.performedByUserId,
            actionType = a.actionType,
            description = a.description,
            timestampMillis = a.timestampMillis,
            previousState = a.previousState,
            newState = a.newState
        )
    }
}
