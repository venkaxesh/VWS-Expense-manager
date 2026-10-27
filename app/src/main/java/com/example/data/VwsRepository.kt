package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class VwsRepository(private val dao: VwsDao) {

    fun getAllUsers(): Flow<List<User>> = dao.getAllUsersFlow().map { list -> list.map { it.toDomain() } }

    fun getAllRooms(): Flow<List<Room>> = dao.getAllRoomsFlow().map { list -> list.map { it.toDomain() } }

    fun getMembersForRoom(roomId: String): Flow<List<RoomMember>> =
        dao.getMembersForRoomFlow(roomId).map { list -> list.map { it.toDomain() } }

    fun getExpensesForRoom(roomId: String): Flow<List<Expense>> =
        dao.getExpensesForRoomFlow(roomId).map { list -> list.map { it.toDomain() } }

    fun getSplitsForRoom(roomId: String): Flow<List<ExpenseSplit>> =
        dao.getSplitsForRoomFlow(roomId).map { list -> list.map { it.toDomain() } }

    fun getPaymentsForRoom(roomId: String): Flow<List<PaymentRecord>> =
        dao.getPaymentsForRoomFlow(roomId).map { list -> list.map { it.toDomain() } }

    fun getNotesForRoom(roomId: String): Flow<List<RoomNote>> =
        dao.getNotesForRoomFlow(roomId).map { list -> list.map { it.toDomain() } }

    fun getAuditLogsForRoom(roomId: String): Flow<List<AuditLog>> =
        dao.getAuditLogsForRoomFlow(roomId).map { list -> list.map { it.toDomain() } }

    suspend fun getUser(userId: String): User? = dao.getUserById(userId)?.toDomain()

    suspend fun saveUser(user: User) {
        dao.insertUser(UserEntity.fromDomain(user))
    }

    suspend fun getRoomById(roomId: String): Room? = dao.getRoomById(roomId)?.toDomain()

    suspend fun getRoomByInvitationCode(code: String): Room? =
        dao.getRoomByInvitationCode(code)?.toDomain()

    suspend fun saveRoom(room: Room) {
        dao.insertRoom(RoomEntity.fromDomain(room))
    }

    suspend fun insertRoomMember(member: RoomMember) {
        dao.insertRoomMember(RoomMemberEntity.fromDomain(member))
    }

    suspend fun createRoom(room: Room, creatorUserId: String, initialMemberIds: List<String>) {
        dao.insertRoom(RoomEntity.fromDomain(room))
        val members = mutableListOf<RoomMember>()
        members.add(RoomMember(UUID.randomUUID().toString(), room.id, creatorUserId, UserRole.ADMIN, MemberStatus.ACTIVE))
        initialMemberIds.filter { it != creatorUserId }.take(5).forEach { memberId ->
            members.add(RoomMember(UUID.randomUUID().toString(), room.id, memberId, UserRole.MEMBER, MemberStatus.ACTIVE))
        }
        dao.insertRoomMembers(members.map { RoomMemberEntity.fromDomain(it) })
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                roomId = room.id,
                performedByUserId = creatorUserId,
                actionType = "ROOM_CREATED",
                description = "Created room '${room.name}' with ${members.size} members."
            )
        )
    }

    suspend fun updateRoom(room: Room, adminUserId: String) {
        dao.updateRoom(RoomEntity.fromDomain(room))
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                roomId = room.id,
                performedByUserId = adminUserId,
                actionType = "ROOM_UPDATED",
                description = "Updated room settings for '${room.name}'."
            )
        )
    }

    suspend fun deleteRoom(roomId: String, performedByUserId: String) {
        dao.deleteRoom(roomId)
        dao.deleteAllMembersForRoom(roomId)
        dao.deleteAllExpensesForRoom(roomId)
        dao.deleteAllSplitsForRoom(roomId)
        dao.deleteAllPaymentsForRoom(roomId)
        dao.deleteAllNotesForRoom(roomId)
        dao.deleteAllAuditLogsForRoom(roomId)
    }

    suspend fun approveMember(roomId: String, userId: String, adminUserId: String) {
        val members = dao.getMembersForRoom(roomId)
        val member = members.find { it.userId == userId }
        if (member != null) {
            dao.updateRoomMember(member.copy(status = MemberStatus.ACTIVE.name))
            dao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    roomId = roomId,
                    performedByUserId = adminUserId,
                    actionType = "MEMBER_APPROVED",
                    description = "Approved member $userId to join room."
                )
            )
        }
    }

    suspend fun removeMember(roomId: String, userId: String, adminUserId: String) {
        dao.deleteRoomMember(roomId, userId)
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                roomId = roomId,
                performedByUserId = adminUserId,
                actionType = "MEMBER_REMOVED",
                description = "Removed member $userId from room."
            )
        )
    }

    /**
     * Exact integer paise calculation for all 4 split types
     */
    fun calculateSplits(
        totalPaise: Long,
        splitType: SplitType,
        selectedMemberIds: List<String>,
        rawInputs: Map<String, String> // memberId -> ratio / percentage / manual amount
    ): Result<List<Pair<String, Long>>> {
        if (totalPaise <= 0L) return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
        if (selectedMemberIds.isEmpty()) return Result.failure(IllegalArgumentException("At least one member must be selected."))

        when (splitType) {
            SplitType.EQUAL -> {
                val count = selectedMemberIds.size
                val base = totalPaise / count
                val remainder = (totalPaise % count).toInt()
                val list = selectedMemberIds.mapIndexed { index, id ->
                    val allocated = base + if (index < remainder) 1L else 0L
                    Pair(id, allocated)
                }
                return Result.success(list)
            }
            SplitType.RATIO -> {
                val ratios = selectedMemberIds.map { id ->
                    val r = rawInputs[id]?.toDoubleOrNull() ?: 1.0
                    if (r <= 0.0) 1.0 else r
                }
                val totalRatio = ratios.sum()
                if (totalRatio <= 0.0) return Result.failure(IllegalArgumentException("Ratios must sum to positive number."))
                var distributed = 0L
                val list = mutableListOf<Pair<String, Long>>()
                selectedMemberIds.forEachIndexed { i, id ->
                    if (i == selectedMemberIds.lastIndex) {
                        val remaining = totalPaise - distributed
                        list.add(Pair(id, remaining))
                    } else {
                        val share = Math.round((totalPaise * ratios[i]) / totalRatio)
                        distributed += share
                        list.add(Pair(id, share))
                    }
                }
                return Result.success(list)
            }
            SplitType.PERCENTAGE -> {
                val percentages = selectedMemberIds.map { id ->
                    rawInputs[id]?.toDoubleOrNull() ?: 0.0
                }
                val totalPct = percentages.sum()
                if (Math.abs(totalPct - 100.0) > 0.01) {
                    return Result.failure(IllegalArgumentException("Percentages must total exactly 100%. Current sum: ${String.format("%.1f", totalPct)}%"))
                }
                var distributed = 0L
                val list = mutableListOf<Pair<String, Long>>()
                selectedMemberIds.forEachIndexed { i, id ->
                    if (i == selectedMemberIds.lastIndex) {
                        val remaining = totalPaise - distributed
                        list.add(Pair(id, remaining))
                    } else {
                        val share = Math.round((totalPaise * percentages[i]) / 100.0)
                        distributed += share
                        list.add(Pair(id, share))
                    }
                }
                return Result.success(list)
            }
            SplitType.SEPARATE_AMOUNT -> {
                val amounts = selectedMemberIds.map { id ->
                    CurrencyHelper.rupeeToPaise(rawInputs[id] ?: "0")
                }
                val sum = amounts.sum()
                if (sum != totalPaise) {
                    val diff = CurrencyHelper.formatPaiseToRupee(Math.abs(sum - totalPaise))
                    val direction = if (sum < totalPaise) "short by" else "exceeds by"
                    return Result.failure(IllegalArgumentException("Amounts do not equal total bill: $direction $diff"))
                }
                val list = selectedMemberIds.zip(amounts)
                return Result.success(list)
            }
        }
    }

    suspend fun createExpenseWithSplits(
        expense: Expense,
        splits: List<Pair<String, Long>>,
        splitValues: Map<String, String>
    ) {
        dao.insertExpense(ExpenseEntity.fromDomain(expense))
        val splitEntities = splits.map { (memberId, amount) ->
            ExpenseSplitEntity(
                id = UUID.randomUUID().toString(),
                expenseId = expense.id,
                roomId = expense.roomId,
                memberUserId = memberId,
                allocatedAmountPaise = amount,
                splitValueInput = splitValues[memberId] ?: "",
                isPaid = (memberId == expense.payerUserId) // original payer automatically credited for their own share
            )
        }
        dao.insertSplits(splitEntities)
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                roomId = expense.roomId,
                performedByUserId = expense.createdByUserId,
                actionType = "EXPENSE_CREATED",
                description = "Added '${expense.description}' for ${CurrencyHelper.formatPaiseToRupee(expense.totalAmountPaise)} (${expense.splitType.displayName})."
            )
        )
    }

    suspend fun recordPayment(payment: PaymentRecord) {
        dao.insertPayment(PaymentRecordEntity.fromDomain(payment))
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                roomId = payment.roomId,
                performedByUserId = payment.fromUserId,
                actionType = "PAYMENT_RECORDED",
                description = "Payment of ${CurrencyHelper.formatPaiseToRupee(payment.amountPaise)} via ${payment.paymentMethod.displayName} (Status: ${payment.status.displayName})."
            )
        )
    }

    suspend fun updatePaymentStatus(paymentId: String, newStatus: PaymentStatus, adminOrRecipientId: String, note: String = "") {
        val entity = dao.getPaymentById(paymentId) ?: return
        val updated = entity.copy(
            status = newStatus.name,
            verifiedAtMillis = if (newStatus == PaymentStatus.PAID) System.currentTimeMillis() else entity.verifiedAtMillis,
            verifiedByUserId = adminOrRecipientId,
            proofNotes = if (note.isNotBlank()) "${entity.proofNotes} | $note" else entity.proofNotes
        )
        dao.updatePayment(updated)
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                roomId = entity.roomId,
                performedByUserId = adminOrRecipientId,
                actionType = "PAYMENT_STATUS_CHANGED",
                description = "Updated payment status to ${newStatus.displayName} for record $paymentId."
            )
        )
    }

    suspend fun createNote(note: RoomNote) {
        dao.insertNote(RoomNoteEntity.fromDomain(note))
    }

    suspend fun markNoteAsRead(noteId: String, isRead: Boolean) {
        // Since we have dao queries, we can update directly
    }

    suspend fun addAuditAdjustment(roomId: String, performedByUserId: String, description: String, amountPaise: Long) {
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                roomId = roomId,
                performedByUserId = performedByUserId,
                actionType = "FINANCIAL_ADJUSTMENT",
                description = "$description (Adjustment: ${CurrencyHelper.formatPaiseToRupee(amountPaise)})"
            )
        )
    }
}
