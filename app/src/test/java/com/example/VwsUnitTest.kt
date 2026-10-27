package com.example

import com.example.data.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

class FakeVwsDao : VwsDao {
    override fun getAllUsersFlow(): Flow<List<UserEntity>> = flowOf(emptyList())
    override suspend fun getUserById(id: String): UserEntity? = null
    override suspend fun getUserByUsername(username: String): UserEntity? = null
    override suspend fun insertUser(user: UserEntity) {}
    override suspend fun insertUsers(users: List<UserEntity>) {}
    override suspend fun updateUser(user: UserEntity) {}
    override fun getAllRoomsFlow(): Flow<List<RoomEntity>> = flowOf(emptyList())
    override suspend fun getRoomById(roomId: String): RoomEntity? = null
    override suspend fun getRoomByInvitationCode(code: String): RoomEntity? = null
    override suspend fun insertRoom(room: RoomEntity) {}
    override suspend fun updateRoom(room: RoomEntity) {}
    override suspend fun deleteRoom(roomId: String) {}
    override suspend fun deleteAllMembersForRoom(roomId: String) {}
    override suspend fun deleteAllExpensesForRoom(roomId: String) {}
    override suspend fun deleteAllSplitsForRoom(roomId: String) {}
    override suspend fun deleteAllPaymentsForRoom(roomId: String) {}
    override suspend fun deleteAllNotesForRoom(roomId: String) {}
    override suspend fun deleteAllAuditLogsForRoom(roomId: String) {}
    override fun getMembersForRoomFlow(roomId: String): Flow<List<RoomMemberEntity>> = flowOf(emptyList())
    override suspend fun getMembersForRoom(roomId: String): List<RoomMemberEntity> = emptyList()
    override suspend fun insertRoomMember(member: RoomMemberEntity) {}
    override suspend fun insertRoomMembers(members: List<RoomMemberEntity>) {}
    override suspend fun updateRoomMember(member: RoomMemberEntity) {}
    override suspend fun deleteRoomMember(roomId: String, userId: String) {}
    override fun getExpensesForRoomFlow(roomId: String): Flow<List<ExpenseEntity>> = flowOf(emptyList())
    override suspend fun getExpenseById(expenseId: String): ExpenseEntity? = null
    override suspend fun insertExpense(expense: ExpenseEntity) {}
    override suspend fun updateExpense(expense: ExpenseEntity) {}
    override suspend fun deleteExpense(expenseId: String) {}
    override fun getSplitsForRoomFlow(roomId: String): Flow<List<ExpenseSplitEntity>> = flowOf(emptyList())
    override fun getSplitsForExpenseFlow(expenseId: String): Flow<List<ExpenseSplitEntity>> = flowOf(emptyList())
    override suspend fun getSplitsForExpense(expenseId: String): List<ExpenseSplitEntity> = emptyList()
    override suspend fun insertSplits(splits: List<ExpenseSplitEntity>) {}
    override suspend fun deleteSplitsForExpense(expenseId: String) {}
    override fun getPaymentsForRoomFlow(roomId: String): Flow<List<PaymentRecordEntity>> = flowOf(emptyList())
    override suspend fun getPaymentById(paymentId: String): PaymentRecordEntity? = null
    override suspend fun insertPayment(payment: PaymentRecordEntity) {}
    override suspend fun insertPayments(payments: List<PaymentRecordEntity>) {}
    override suspend fun updatePayment(payment: PaymentRecordEntity) {}
    override fun getNotesForRoomFlow(roomId: String): Flow<List<RoomNoteEntity>> = flowOf(emptyList())
    override suspend fun insertNote(note: RoomNoteEntity) {}
    override suspend fun insertNotes(notes: List<RoomNoteEntity>) {}
    override suspend fun updateNote(note: RoomNoteEntity) {}
    override suspend fun deleteNote(noteId: String) {}
    override fun getAuditLogsForRoomFlow(roomId: String): Flow<List<AuditLogEntity>> = flowOf(emptyList())
    override suspend fun insertAuditLog(log: AuditLogEntity) {}
    override suspend fun insertAuditLogs(logs: List<AuditLogEntity>) {}
}

class VwsUnitTest {

    private val fakeDao = FakeVwsDao()
    private val repository = VwsRepository(fakeDao)

    @Test
    fun testCurrencyFormatting() {
        assertEquals("₹2,480", CurrencyHelper.formatPaiseToRupee(248000L))
        assertEquals("₹2,480.50", CurrencyHelper.formatPaiseToRupee(248050L))
        assertEquals("₹0", CurrencyHelper.formatPaiseToRupee(0L))
        assertEquals("-₹500", CurrencyHelper.formatPaiseToRupee(-50000L))

        assertEquals(248000L, CurrencyHelper.rupeeToPaise("2480"))
        assertEquals(248050L, CurrencyHelper.rupeeToPaise("2,480.50"))
        assertEquals(119900L, CurrencyHelper.rupeeToPaise("1199"))
    }

    @Test
    fun testEqualSplitWithDeterministicRemainder() {
        val members = listOf("user_1", "user_2", "user_3")
        val result = repository.calculateSplits(
            totalPaise = 1000L,
            splitType = SplitType.EQUAL,
            selectedMemberIds = members,
            rawInputs = emptyMap()
        )
        assertTrue(result.isSuccess)
        val splits = result.getOrNull()!!
        assertEquals(3, splits.size)
        assertEquals(1000L, splits.sumOf { it.second })
        assertEquals(334L, splits[0].second)
        assertEquals(333L, splits[1].second)
        assertEquals(333L, splits[2].second)
    }

    @Test
    fun testRatioSplit() {
        val members = listOf("user_1", "user_2", "user_3")
        val inputs = mapOf("user_1" to "1", "user_2" to "2", "user_3" to "1")
        val result = repository.calculateSplits(
            totalPaise = 40000L,
            splitType = SplitType.RATIO,
            selectedMemberIds = members,
            rawInputs = inputs
        )
        assertTrue(result.isSuccess)
        val splits = result.getOrNull()!!
        assertEquals(40000L, splits.sumOf { it.second })
        assertEquals(10000L, splits[0].second)
        assertEquals(20000L, splits[1].second)
        assertEquals(10000L, splits[2].second)
    }

    @Test
    fun testPercentageSplit() {
        val members = listOf("user_1", "user_2", "user_3")
        val inputs = mapOf("user_1" to "50", "user_2" to "25", "user_3" to "25")
        val result = repository.calculateSplits(
            totalPaise = 1000000L,
            splitType = SplitType.PERCENTAGE,
            selectedMemberIds = members,
            rawInputs = inputs
        )
        assertTrue(result.isSuccess)
        val splits = result.getOrNull()!!
        assertEquals(1000000L, splits.sumOf { it.second })
        assertEquals(500000L, splits[0].second)
        assertEquals(250000L, splits[1].second)
        assertEquals(250000L, splits[2].second)
    }

    @Test
    fun testSeparateAmountValidation() {
        val members = listOf("user_1", "user_2")
        val inputs = mapOf("user_1" to "600", "user_2" to "400")
        val result = repository.calculateSplits(
            totalPaise = 100000L,
            splitType = SplitType.SEPARATE_AMOUNT,
            selectedMemberIds = members,
            rawInputs = inputs
        )
        assertTrue(result.isSuccess)
        assertEquals(100000L, result.getOrNull()!!.sumOf { it.second })

        val badInputs = mapOf("user_1" to "500", "user_2" to "400")
        val badResult = repository.calculateSplits(
            totalPaise = 100000L,
            splitType = SplitType.SEPARATE_AMOUNT,
            selectedMemberIds = members,
            rawInputs = badInputs
        )
        assertTrue(badResult.isFailure)
    }

    @Test
    fun testBankAccountMasking() {
        val user = User(
            id = "u1",
            username = "venkaxesh",
            fullName = "Venkatesh",
            email = "v@test.com",
            phone = "9876543210",
            upiId = "v@okaxis",
            bankAccountNumber = "109823485721",
            ifscCode = "HDFC0001",
            role = UserRole.ADMIN
        )
        assertEquals("XXXX-XXXX-5721", user.maskedAccountNumber)
    }
}
