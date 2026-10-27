package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VwsDao {
    // Users
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    // Rooms
    @Query("SELECT * FROM rooms WHERE isArchived = 0 ORDER BY createdAtMillis DESC")
    fun getAllRoomsFlow(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :roomId LIMIT 1")
    suspend fun getRoomById(roomId: String): RoomEntity?

    @Query("SELECT * FROM rooms WHERE invitationCode = :code OR id = :code LIMIT 1")
    suspend fun getRoomByInvitationCode(code: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :roomId")
    suspend fun deleteRoom(roomId: String)

    @Query("DELETE FROM room_members WHERE roomId = :roomId")
    suspend fun deleteAllMembersForRoom(roomId: String)

    @Query("DELETE FROM expenses WHERE roomId = :roomId")
    suspend fun deleteAllExpensesForRoom(roomId: String)

    @Query("DELETE FROM expense_splits WHERE roomId = :roomId")
    suspend fun deleteAllSplitsForRoom(roomId: String)

    @Query("DELETE FROM payment_records WHERE roomId = :roomId")
    suspend fun deleteAllPaymentsForRoom(roomId: String)

    @Query("DELETE FROM room_notes WHERE roomId = :roomId")
    suspend fun deleteAllNotesForRoom(roomId: String)

    @Query("DELETE FROM audit_logs WHERE roomId = :roomId")
    suspend fun deleteAllAuditLogsForRoom(roomId: String)

    // Room Members
    @Query("SELECT * FROM room_members WHERE roomId = :roomId")
    fun getMembersForRoomFlow(roomId: String): Flow<List<RoomMemberEntity>>

    @Query("SELECT * FROM room_members WHERE roomId = :roomId")
    suspend fun getMembersForRoom(roomId: String): List<RoomMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomMember(member: RoomMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomMembers(members: List<RoomMemberEntity>)

    @Update
    suspend fun updateRoomMember(member: RoomMemberEntity)

    @Query("DELETE FROM room_members WHERE roomId = :roomId AND userId = :userId")
    suspend fun deleteRoomMember(roomId: String, userId: String)

    // Expenses
    @Query("SELECT * FROM expenses WHERE roomId = :roomId ORDER BY timestamp DESC, createdAtMillis DESC")
    fun getExpensesForRoomFlow(roomId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE expenseId = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE expenseId = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    // Expense Splits
    @Query("SELECT * FROM expense_splits WHERE roomId = :roomId")
    fun getSplitsForRoomFlow(roomId: String): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsForExpenseFlow(expenseId: String): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpense(expenseId: String): List<ExpenseSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    // Payment Records
    @Query("SELECT * FROM payment_records WHERE roomId = :roomId ORDER BY submittedAtMillis DESC")
    fun getPaymentsForRoomFlow(roomId: String): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE id = :paymentId LIMIT 1")
    suspend fun getPaymentById(paymentId: String): PaymentRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentRecordEntity>)

    @Update
    suspend fun updatePayment(payment: PaymentRecordEntity)

    // Notes
    @Query("SELECT * FROM room_notes WHERE roomId = :roomId ORDER BY createdAtMillis DESC")
    fun getNotesForRoomFlow(roomId: String): Flow<List<RoomNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: RoomNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<RoomNoteEntity>)

    @Update
    suspend fun updateNote(note: RoomNoteEntity)

    @Query("DELETE FROM room_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    // Audit Logs
    @Query("SELECT * FROM audit_logs WHERE roomId = :roomId ORDER BY timestampMillis DESC")
    fun getAuditLogsForRoomFlow(roomId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)
}
