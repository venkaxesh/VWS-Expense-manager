package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VwsDatabase
import com.example.data.VwsFirestoreRepository
import com.example.data.VwsRepository
import com.example.model.*
import com.example.ui.util.ReminderNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab {
    HOME,
    TRANSACTIONS,
    NOTES
}

data class UserFinancialSummary(
    val allocatedSharePaise: Long = 0L,
    val billsPaidUpfrontPaise: Long = 0L,
    val paymentsSentPaise: Long = 0L,
    val paymentsReceivedPaise: Long = 0L,
    val outstandingBalancePaise: Long = 0L,
    val receivableBalancePaise: Long = 0L
) {
    val totalPaidPaise: Long get() = billsPaidUpfrontPaise + paymentsSentPaise
}

data class VwsUiState(
    val currentUserId: String = "",
    val currentRoomId: String = "",
    val currentTab: AppTab = AppTab.HOME,
    val users: List<User> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val members: List<RoomMember> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val splits: List<ExpenseSplit> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val notes: List<RoomNote> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList(),
    val financialSummary: UserFinancialSummary = UserFinancialSummary(),
    val settlementSuggestions: List<SettlementSuggestion> = emptyList(),
    val overdueBalancePaise: Long = 0L,
    val unreadNotesCount: Int = 0,
    val reminderNextRunMillis: Long = System.currentTimeMillis() + (3 * 3600000L),
    val isQuietHoursActive: Boolean = false,
    val isAddExpenseOpen: Boolean = false,
    val isRecordPaymentOpen: Boolean = false,
    val isCalendarReportOpen: Boolean = false,
    val isAdminDashboardOpen: Boolean = false,
    val isCreateRoomOpen: Boolean = false,
    val isJoinRoomOpen: Boolean = false,
    val isProfileOpen: Boolean = false,
    val activeExpenseDetail: Expense? = null,
    val toastMessage: String? = null
) {
    val currentUser: User? get() = users.find { it.id == currentUserId }
    val currentRoom: Room? get() = rooms.find { it.id == currentRoomId }
    val isCurrentAdmin: Boolean get() = currentUser?.role == UserRole.ADMIN || currentRoom?.adminUid == currentUserId
}

class VwsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VwsRepository
    private val firestoreRepository: VwsFirestoreRepository

    private val _currentUserId = MutableStateFlow(FirebaseAuth.getInstance().currentUser?.uid ?: "")
    private val _currentRoomId = MutableStateFlow("")
    private val _currentTab = MutableStateFlow(AppTab.HOME)

    private val _isAddExpenseOpen = MutableStateFlow(false)
    private val _isRecordPaymentOpen = MutableStateFlow(false)
    private val _isCalendarReportOpen = MutableStateFlow(false)
    private val _isAdminDashboardOpen = MutableStateFlow(false)
    private val _isCreateRoomOpen = MutableStateFlow(false)
    private val _isJoinRoomOpen = MutableStateFlow(false)
    private val _isProfileOpen = MutableStateFlow(false)
    private val _activeExpenseDetail = MutableStateFlow<Expense?>(null)
    private val _toastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VwsUiState>

    private data class NavState(val userId: String, val roomId: String, val tab: AppTab)
    private data class GlobalState(val users: List<User>, val rooms: List<Room>)
    private data class LedgerState(val expenses: List<Expense>, val splits: List<ExpenseSplit>, val payments: List<PaymentRecord>)
    private data class RoomMetaState(val members: List<RoomMember>, val notes: List<RoomNote>, val auditLogs: List<AuditLog>)
    private data class DialogState(
        val isAddExpenseOpen: Boolean,
        val isRecordPaymentOpen: Boolean,
        val isCalendarReportOpen: Boolean,
        val isAdminDashboardOpen: Boolean,
        val isCreateRoomOpen: Boolean,
        val isJoinRoomOpen: Boolean,
        val isProfileOpen: Boolean,
        val activeExpenseDetail: Expense?,
        val toastMessage: String?
    )

    init {
        val dao = VwsDatabase.getDatabase(application).vwsDao()
        repository = VwsRepository(dao)
        firestoreRepository = VwsFirestoreRepository(application)

        val navFlow = combine(_currentUserId, _currentRoomId, _currentTab) { u, r, t ->
            NavState(u, r, t)
        }

        val globalFlow = combine(repository.getAllUsers(), repository.getAllRooms()) { u, r ->
            if (_currentRoomId.value.isEmpty() && r.isNotEmpty()) {
                _currentRoomId.value = r.first().id
            }
            GlobalState(u, r)
        }

        val expensesFlow = _currentRoomId.flatMapLatest { repository.getExpensesForRoom(it) }
        val splitsFlow = _currentRoomId.flatMapLatest { repository.getSplitsForRoom(it) }
        val paymentsFlow = _currentRoomId.flatMapLatest { repository.getPaymentsForRoom(it) }

        val ledgerFlow = combine(expensesFlow, splitsFlow, paymentsFlow) { exp, sp, pay ->
            LedgerState(exp, sp, pay)
        }

        val membersFlow = _currentRoomId.flatMapLatest { repository.getMembersForRoom(it) }
        val notesFlow = _currentRoomId.flatMapLatest { repository.getNotesForRoom(it) }
        val auditLogsFlow = _currentRoomId.flatMapLatest { repository.getAuditLogsForRoom(it) }

        val roomMetaFlow = combine(membersFlow, notesFlow, auditLogsFlow) { mem, not, aud ->
            RoomMetaState(mem, not, aud)
        }

        val dialogsFlow = combine(
            combine(_isAddExpenseOpen, _isRecordPaymentOpen, _isCalendarReportOpen) { a, b, c -> Triple(a, b, c) },
            combine(_isAdminDashboardOpen, _isCreateRoomOpen, _isJoinRoomOpen, _isProfileOpen) { d, e, f, g ->
                listOf(d, e, f, g)
            },
            combine(_activeExpenseDetail, _toastMessage) { h, i -> Pair(h, i) }
        ) { (addExp, recPay, cal), adminRoomList, (expDet, toast) ->
            DialogState(
                isAddExpenseOpen = addExp,
                isRecordPaymentOpen = recPay,
                isCalendarReportOpen = cal,
                isAdminDashboardOpen = adminRoomList[0],
                isCreateRoomOpen = adminRoomList[1],
                isJoinRoomOpen = adminRoomList[2],
                isProfileOpen = adminRoomList[3],
                activeExpenseDetail = expDet,
                toastMessage = toast
            )
        }

        uiState = combine(navFlow, globalFlow, ledgerFlow, roomMetaFlow, dialogsFlow) { nav, global, ledger, meta, dialogs ->
            val (summary, suggestions, overdue) = computeBalancesAndSettlements(
                userId = nav.userId,
                users = global.users,
                expenses = ledger.expenses,
                splits = ledger.splits,
                payments = ledger.payments
            )

            val unreadCount = meta.notes.count {
                !it.isRead && (!it.isPrivate || it.authorUserId == nav.userId || it.targetUserId == nav.userId)
            }

            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val isQuiet = currentHour in 23..24 || currentHour in 0..7

            VwsUiState(
                currentUserId = nav.userId,
                currentRoomId = nav.roomId,
                currentTab = nav.tab,
                users = global.users,
                rooms = global.rooms,
                members = meta.members,
                expenses = ledger.expenses,
                splits = ledger.splits,
                payments = ledger.payments,
                notes = meta.notes,
                auditLogs = meta.auditLogs,
                financialSummary = summary,
                settlementSuggestions = suggestions,
                overdueBalancePaise = overdue,
                unreadNotesCount = unreadCount,
                isQuietHoursActive = isQuiet,
                isAddExpenseOpen = dialogs.isAddExpenseOpen,
                isRecordPaymentOpen = dialogs.isRecordPaymentOpen,
                isCalendarReportOpen = dialogs.isCalendarReportOpen,
                isAdminDashboardOpen = dialogs.isAdminDashboardOpen,
                isCreateRoomOpen = dialogs.isCreateRoomOpen,
                isJoinRoomOpen = dialogs.isJoinRoomOpen,
                isProfileOpen = dialogs.isProfileOpen,
                activeExpenseDetail = dialogs.activeExpenseDetail,
                toastMessage = dialogs.toastMessage
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            VwsUiState()
        )

        viewModelScope.launch {
            try {
                firestoreRepository.observeRooms().collect { remoteList ->
                    remoteList.forEach { data ->
                        val roomId = data["id"] as? String ?: return@forEach
                        val name = data["name"] as? String ?: "Bachelor Room"
                        val code = data["code"] as? String ?: ""
                        val adminUid = data["adminUid"] as? String ?: ""
                        val address = data["address"] as? String ?: ""
                        val ebConsumerNumber = data["ebConsumerNumber"] as? String ?: ""
                        val waterTaxNumber = data["waterTaxNumber"] as? String ?: ""
                        val ownerName = data["ownerName"] as? String ?: ""
                        val ownerPhone = data["ownerPhone"] as? String ?: ""
                        val monthlyRentPaise = (data["monthlyRentPaise"] as? Number)?.toLong() ?: 0L
                        val rentDueDay = (data["rentDueDay"] as? Number)?.toInt() ?: 5
                        val description = data["description"] as? String ?: ""
                        val memberUids = (data["memberUids"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                        val currentRooms = repository.getAllRooms().first()
                        if (currentRooms.none { it.id == roomId }) {
                            val r = Room(
                                id = roomId,
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
                                invitationCode = code
                            )
                            repository.createRoom(r, adminUid, memberUids)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore real-time room sync: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                firestoreRepository.observeExpenses().collect { remoteList ->
                    remoteList.forEach { data ->
                        val expenseId = data["id"] as? String ?: return@forEach
                        val roomId = data["roomId"] as? String ?: ""
                        val title = data["title"] as? String ?: (data["description"] as? String ?: "Expense")
                        val amountPaise = (data["amountPaise"] as? Number)?.toLong() ?: 0L
                        val paidByUid = data["paidByUid"] as? String ?: ""
                        val dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        val notes = data["notes"] as? String ?: ""
                        val splitTypeName = data["splitType"] as? String ?: "EQUAL"
                        val splitType = try { SplitType.valueOf(splitTypeName) } catch (e: Exception) { SplitType.EQUAL }

                        val currentExpenses = repository.getExpensesForRoom(roomId).first()
                        if (currentExpenses.none { it.id == expenseId }) {
                            val exp = Expense(
                                id = expenseId,
                                roomId = roomId,
                                createdByUserId = paidByUid,
                                payerUserId = paidByUid,
                                description = title,
                                category = ExpenseCategory.OTHER,
                                totalAmountPaise = amountPaise,
                                expenseDateMillis = dateMillis,
                                splitType = splitType,
                                notes = notes
                            )
                            val participantUids = (data["participantUids"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            val splits = if (participantUids.isNotEmpty()) {
                                val splitAmount = amountPaise / participantUids.size
                                participantUids.map { uid -> Pair(uid, splitAmount) }
                            } else emptyList()
                            repository.createExpenseWithSplits(exp, splits, emptyMap())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore real-time expense sync: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                firestoreRepository.observeNotes().collect { remoteList ->
                    remoteList.forEach { data ->
                        val noteId = data["id"] as? String ?: return@forEach
                        val roomId = data["roomId"] as? String ?: ""
                        val authorUserId = data["authorUserId"] as? String ?: ""
                        val title = data["title"] as? String ?: "Note"
                        val content = data["content"] as? String ?: ""
                        val noteTypeName = data["noteType"] as? String ?: "SHARED_ROOM"
                        val noteType = try { NoteType.valueOf(noteTypeName) } catch (e: Exception) { NoteType.SHARED_ROOM }
                        val isPrivate = (data["isPrivate"] as? Boolean) ?: false
                        val targetUserId = (data["targetUserId"] as? String)?.takeIf { it.isNotBlank() }
                        val dueDateMillis = (data["dueDateMillis"] as? Number)?.toLong()?.takeIf { it > 0 }

                        val currentNotes = repository.getNotesForRoom(roomId).first()
                        if (currentNotes.none { it.id == noteId }) {
                            val newNote = RoomNote(
                                id = noteId,
                                roomId = roomId,
                                authorUserId = authorUserId,
                                title = title,
                                content = content,
                                noteType = noteType,
                                isPrivate = isPrivate,
                                targetUserId = targetUserId,
                                dueDateMillis = dueDateMillis
                            )
                            repository.createNote(newNote)

                            // Trigger notification with reminder tone if note is from another user and relevant to current user
                            if (authorUserId != _currentUserId.value && (targetUserId == null || targetUserId == _currentUserId.value)) {
                                ReminderNotificationHelper.sendReminderNotification(
                                    context = getApplication(),
                                    title = "Kaasu Kudu Kaasu: $title",
                                    message = content.ifBlank { "New roommate note alert" }
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore real-time note sync: ${e.message}")
            }
        }
    }

    private fun computeBalancesAndSettlements(
        userId: String,
        users: List<User>,
        expenses: List<Expense>,
        splits: List<ExpenseSplit>,
        payments: List<PaymentRecord>
    ): Triple<UserFinancialSummary, List<SettlementSuggestion>, Long> {
        val userMap = users.associateBy { it.id }

        // 1. Total allocated share across all expenses for this user
        val myAllocated = splits.filter { it.memberUserId == userId }.sumOf { it.allocatedAmountPaise }

        // 2. Total bills paid upfront by this user
        val myUpfront = expenses.filter { it.payerUserId == userId }.sumOf { it.totalAmountPaise }

        // 3. Direct peer payments sent and received (status = PAID)
        val paidPayments = payments.filter { it.status == PaymentStatus.PAID }
        val mySentPayments = paidPayments.filter { it.fromUserId == userId }.sumOf { it.amountPaise }
        val myReceivedPayments = paidPayments.filter { it.toUserId == userId }.sumOf { it.amountPaise }

        // Pairwise net ledger
        val netBalanceMap = mutableMapOf<String, Long>()
        users.forEach { u -> netBalanceMap[u.id] = 0L }

        expenses.forEach { exp ->
            netBalanceMap[exp.payerUserId] = (netBalanceMap[exp.payerUserId] ?: 0L) + exp.totalAmountPaise
        }
        splits.forEach { split ->
            netBalanceMap[split.memberUserId] = (netBalanceMap[split.memberUserId] ?: 0L) - split.allocatedAmountPaise
        }
        paidPayments.forEach { pay ->
            netBalanceMap[pay.fromUserId] = (netBalanceMap[pay.fromUserId] ?: 0L) + pay.amountPaise
            netBalanceMap[pay.toUserId] = (netBalanceMap[pay.toUserId] ?: 0L) - pay.amountPaise
        }

        val myNet = netBalanceMap[userId] ?: 0L
        val outstanding = if (myNet < 0L) -myNet else 0L
        val receivable = if (myNet > 0L) myNet else 0L

        // Compute settlement graph (greedy min-cash-flow)
        val debtors = mutableListOf<Pair<String, Long>>()
        val creditors = mutableListOf<Pair<String, Long>>()

        netBalanceMap.forEach { (uid, net) ->
            if (net < 0L) debtors.add(Pair(uid, -net))
            else if (net > 0L) creditors.add(Pair(uid, net))
        }

        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val suggestions = mutableListOf<SettlementSuggestion>()
        var dIdx = 0
        var cIdx = 0
        val debtorBalances = debtors.map { it.second }.toMutableList()
        val creditorBalances = creditors.map { it.second }.toMutableList()

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val dUid = debtors[dIdx].first
            val cUid = creditors[cIdx].first
            val dBal = debtorBalances[dIdx]
            val cBal = creditorBalances[cIdx]

            val settleAmount = Math.min(dBal, cBal)
            if (settleAmount > 0L) {
                val dName = userMap[dUid]?.fullName ?: dUid
                val cUser = userMap[cUid]
                suggestions.add(
                    SettlementSuggestion(
                        fromUserId = dUid,
                        fromUserName = dName,
                        toUserId = cUid,
                        toUserName = cUser?.fullName ?: cUid,
                        amountPaise = settleAmount,
                        toUserUpiId = cUser?.upiId ?: ""
                    )
                )
            }

            debtorBalances[dIdx] -= settleAmount
            creditorBalances[cIdx] -= settleAmount

            if (debtorBalances[dIdx] <= 0L) dIdx++
            if (creditorBalances[cIdx] <= 0L) cIdx++
        }

        // Overdue balance: check unpaid expenses older than 7 days
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 86400000L)
        val overduePaise = splits
            .filter { it.memberUserId == userId && !it.isPaid }
            .filter { split ->
                val exp = expenses.find { it.id == split.expenseId }
                exp != null && exp.expenseDateMillis < sevenDaysAgo
            }
            .sumOf { it.allocatedAmountPaise }

        val summary = UserFinancialSummary(
            allocatedSharePaise = myAllocated,
            billsPaidUpfrontPaise = myUpfront,
            paymentsSentPaise = mySentPayments,
            paymentsReceivedPaise = myReceivedPayments,
            outstandingBalancePaise = outstanding,
            receivableBalancePaise = receivable
        )

        return Triple(summary, suggestions, overduePaise)
    }

    // Tab & Navigation
    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun switchRoom(roomId: String) {
        _currentRoomId.value = roomId
        showToast("Switched active room")
    }

    fun switchUser(userId: String) {
        _currentUserId.value = userId
        val user = uiState.value.users.find { it.id == userId }
        showToast("Logged in as ${user?.fullName}")
    }

    // Dialog state toggles
    fun openAddExpense(open: Boolean) { _isAddExpenseOpen.value = open }
    fun openRecordPayment(open: Boolean) { _isRecordPaymentOpen.value = open }
    fun openCalendarReport(open: Boolean) { _isCalendarReportOpen.value = open }
    fun openAdminDashboard(open: Boolean) { _isAdminDashboardOpen.value = open }
    fun openCreateRoom(open: Boolean) { _isCreateRoomOpen.value = open }
    fun openJoinRoom(open: Boolean) { _isJoinRoomOpen.value = open }
    fun openProfile(open: Boolean) { _isProfileOpen.value = open }
    fun setExpenseDetail(expense: Expense?) { _activeExpenseDetail.value = expense }
    fun showToast(msg: String) { _toastMessage.value = msg }
    fun clearToast() { _toastMessage.value = null }

    // Actions
    fun createRoom(
        name: String,
        address: String,
        ebNumber: String,
        waterNumber: String,
        ownerName: String,
        ownerPhone: String,
        monthlyRentRupee: String,
        rentDueDay: Int,
        description: String,
        selectedMemberIds: List<String>
    ) {
        viewModelScope.launch {
            val rentPaise = CurrencyHelper.rupeeToPaise(monthlyRentRupee)
            val autoCode = (100000..999999).random().toString()
            val newRoom = Room(
                id = "room_${UUID.randomUUID().toString().take(8)}",
                name = name.ifBlank { "Bachelor Room" },
                address = address,
                ebConsumerNumber = ebNumber,
                waterTaxNumber = waterNumber,
                ownerName = ownerName,
                ownerPhone = ownerPhone,
                monthlyRentPaise = rentPaise,
                rentDueDay = rentDueDay.coerceIn(1, 31),
                description = description,
                adminUid = _currentUserId.value,
                invitationCode = autoCode
            )
            repository.createRoom(newRoom, _currentUserId.value, selectedMemberIds)
            try {
                firestoreRepository.createRoom(newRoom, _currentUserId.value)
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore createRoom error: ${e.message}")
            }
            _currentRoomId.value = newRoom.id
            _isCreateRoomOpen.value = false
            showToast("Room '${newRoom.name}' created! Code: $autoCode")
        }
    }

    fun deleteRoom(roomId: String) {
        val room = uiState.value.rooms.find { it.id == roomId }
        val myId = _currentUserId.value
        val isCreator = room?.adminUid == myId || uiState.value.isCurrentAdmin
        if (!isCreator) {
            showToast("Only room creator or admin can delete this room")
            return
        }
        viewModelScope.launch {
            repository.deleteRoom(roomId, myId)
            try {
                firestoreRepository.deleteRoom(roomId)
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore deleteRoom error: ${e.message}")
            }
            val remainingRooms = repository.getAllRooms().first()
            _currentRoomId.value = remainingRooms.firstOrNull()?.id ?: ""
            showToast("Room '${room?.name ?: ""}' deleted successfully")
        }
    }

    fun joinRoomByCode(code: String) {
        val cleanCode = code.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            showToast("Please enter a valid 6-digit numeric room code")
            return
        }
        viewModelScope.launch {
            var targetRoom = repository.getRoomByInvitationCode(cleanCode)
            if (targetRoom == null) {
                try {
                    targetRoom = firestoreRepository.getRoomByCode(cleanCode)
                    if (targetRoom != null) {
                        repository.saveRoom(targetRoom)
                    }
                } catch (e: Exception) {
                    Log.e("VwsVM", "Firestore getRoomByCode error: ${e.message}")
                }
            }

            if (targetRoom == null) {
                showToast("No room found with code: $cleanCode")
                return@launch
            }

            val userId = _currentUserId.value
            val member = RoomMember(
                id = "mem_${UUID.randomUUID().toString().take(8)}",
                roomId = targetRoom.id,
                userId = userId,
                role = UserRole.MEMBER,
                status = MemberStatus.PENDING_APPROVAL,
                joinedAtMillis = System.currentTimeMillis()
            )
            repository.insertRoomMember(member)
            try {
                firestoreRepository.addRoomMember(targetRoom.id, userId)
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore addRoomMember error: ${e.message}")
            }
            _currentRoomId.value = targetRoom.id
            _isJoinRoomOpen.value = false
            showToast("Joined '${targetRoom.name}'! Pending creator approval.")
        }
    }

    fun syncUserWithAuth(authUser: FirebaseUser) {
        _currentUserId.value = authUser.uid
        viewModelScope.launch {
            val localUser = repository.getUser(authUser.uid)
            if (localUser == null) {
                val remoteProfile = firestoreRepository.getUserProfile(authUser.uid)
                val userToSave = remoteProfile ?: User(
                    id = authUser.uid,
                    username = authUser.email?.substringBefore("@") ?: "user",
                    fullName = authUser.displayName ?: authUser.email?.substringBefore("@") ?: "User",
                    email = authUser.email ?: "",
                    role = UserRole.ADMIN,
                    isApproved = true
                )
                repository.saveUser(userToSave)
                try {
                    firestoreRepository.saveUserProfile(userToSave)
                } catch (e: Exception) {
                    Log.e("VwsVM", "Sync user profile: ${e.message}")
                }
            }
            try {
                val remoteRooms = firestoreRepository.getRoomsForUser(authUser.uid)
                remoteRooms.forEach { room ->
                    if (repository.getAllRooms().first().none { it.id == room.id }) {
                        repository.createRoom(room, room.adminUid, listOf(authUser.uid))
                    }
                }
                if (_currentRoomId.value.isEmpty() && remoteRooms.isNotEmpty()) {
                    _currentRoomId.value = remoteRooms.first().id
                }
            } catch (e: Exception) {
                Log.e("VwsVM", "Sync rooms error: ${e.message}")
            }
        }
    }

    fun syncLocalUser(userId: String) {
        _currentUserId.value = userId
        viewModelScope.launch {
            try {
                val remoteProfile = firestoreRepository.getUserProfile(userId)
                if (remoteProfile != null) {
                    repository.saveUser(remoteProfile)
                }
                val remoteRooms = firestoreRepository.getRoomsForUser(userId)
                remoteRooms.forEach { room ->
                    if (repository.getAllRooms().first().none { it.id == room.id }) {
                        repository.createRoom(room, room.adminUid, listOf(userId))
                    }
                }
                if (_currentRoomId.value.isEmpty() && remoteRooms.isNotEmpty()) {
                    _currentRoomId.value = remoteRooms.first().id
                }
            } catch (e: Exception) {
                Log.e("VwsVM", "Sync local user firestore error: ${e.message}")
            }
            val allRooms = repository.getAllRooms().first()
            if (_currentRoomId.value.isEmpty() && allRooms.isNotEmpty()) {
                _currentRoomId.value = allRooms.first().id
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        _currentUserId.value = ""
        _currentRoomId.value = ""
        onComplete()
        showToast("Signed out successfully")
    }

    fun addExpense(
        description: String,
        amountRupees: String,
        category: ExpenseCategory,
        customCategory: String,
        splitType: SplitType,
        payerUserId: String,
        expenseDateMillis: Long,
        selectedMemberIds: List<String>,
        splitInputs: Map<String, String>,
        notes: String
    ) {
        viewModelScope.launch {
            val totalPaise = CurrencyHelper.rupeeToPaise(amountRupees)
            if (totalPaise <= 0L) {
                showToast("Please enter a valid expense amount.")
                return@launch
            }

            val result = repository.calculateSplits(totalPaise, splitType, selectedMemberIds, splitInputs)
            result.fold(
                onSuccess = { splitsList ->
                    val expense = Expense(
                        id = "exp_${UUID.randomUUID().toString().take(8)}",
                        roomId = _currentRoomId.value,
                        createdByUserId = _currentUserId.value,
                        payerUserId = payerUserId,
                        description = description.ifBlank { "Room Expense" },
                        category = category,
                        customCategoryName = if (category == ExpenseCategory.OTHER) customCategory else null,
                        totalAmountPaise = totalPaise,
                        expenseDateMillis = expenseDateMillis,
                        splitType = splitType,
                        notes = notes
                    )
                    repository.createExpenseWithSplits(expense, splitsList, splitInputs)
                    try {
                        firestoreRepository.addExpense(expense, selectedMemberIds)
                    } catch (e: Exception) {
                        Log.e("VwsVM", "Firestore addExpense error: ${e.message}")
                    }
                    _isAddExpenseOpen.value = false
                    showToast("Expense '${expense.description}' added successfully!")
                },
                onFailure = { error ->
                    showToast(error.message ?: "Invalid split configuration")
                }
            )
        }
    }

    fun submitPayment(
        toUserId: String,
        amountRupees: String,
        method: PaymentMethod,
        referenceId: String,
        notes: String
    ) {
        viewModelScope.launch {
            val amountPaise = CurrencyHelper.rupeeToPaise(amountRupees)
            if (amountPaise <= 0L) {
                showToast("Please enter a valid payment amount.")
                return@launch
            }
            if (toUserId.isBlank()) {
                showToast("Please select a recipient roommate.")
                return@launch
            }

            // DUPLICATE PAYMENT REJECTION
            val currentPayments = repository.getPaymentsForRoom(_currentRoomId.value).first()
            val trimmedRef = referenceId.trim()
            if (trimmedRef.isNotBlank()) {
                val hasDuplicateRef = currentPayments.any { p ->
                    p.referenceId.isNotBlank() && p.referenceId.trim().equals(trimmedRef, ignoreCase = true)
                }
                if (hasDuplicateRef) {
                    showToast("Duplicate payment rejected! Reference ID already recorded.")
                    return@launch
                }
            }

            // Reject identical recent payment (within 5 minutes)
            val now = System.currentTimeMillis()
            val duplicateRecent = currentPayments.any { p ->
                p.fromUserId == _currentUserId.value &&
                p.toUserId == toUserId &&
                p.amountPaise == amountPaise &&
                (now - p.submittedAtMillis) < 300_000L
            }
            if (duplicateRecent) {
                showToast("Duplicate payment rejected! Identical payment already recorded.")
                return@launch
            }

            val payment = PaymentRecord(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                roomId = _currentRoomId.value,
                fromUserId = _currentUserId.value,
                toUserId = toUserId,
                amountPaise = amountPaise,
                paymentMethod = method,
                referenceId = referenceId,
                status = if (method == PaymentMethod.CASH && _currentUserId.value == toUserId) PaymentStatus.PAID else PaymentStatus.PAYMENT_SUBMITTED,
                proofNotes = notes
            )
            repository.recordPayment(payment)
            try {
                firestoreRepository.recordTransaction(payment)
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore recordTransaction error: ${e.message}")
            }
            _isRecordPaymentOpen.value = false
            showToast("Payment of ${CurrencyHelper.formatPaiseToRupee(amountPaise)} submitted!")
        }
    }

    fun verifyPayment(paymentId: String, newStatus: PaymentStatus, note: String = "") {
        viewModelScope.launch {
            repository.updatePaymentStatus(paymentId, newStatus, _currentUserId.value, note)
            showToast("Payment status updated to ${newStatus.displayName}")
        }
    }

    fun launchUpiPayment(context: Context, payeeUpi: String, payeeName: String, amountPaise: Long, note: String) {
        val amountRupee = String.format(java.util.Locale.US, "%.2f", amountPaise / 100.0)
        val uriStr = "upi://pay?pa=$payeeUpi&pn=${Uri.encode(payeeName)}&am=$amountRupee&cu=INR&tn=${Uri.encode(note)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
        try {
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            context.startActivity(chooser)
        } catch (e: Exception) {
            showToast("No UPI payment app found. You can copy UPI ID: $payeeUpi")
        }
    }

    fun launchGooglePay(context: Context, payeeUpi: String, payeeName: String, amountPaise: Long, note: String) {
        val amountRupee = String.format(java.util.Locale.US, "%.2f", amountPaise / 100.0)
        val cleanUpi = payeeUpi.ifBlank { "vwsroom@upi" }
        val uriStr = "upi://pay?pa=$cleanUpi&pn=${Uri.encode(payeeName.ifBlank { "Roommate" })}&am=$amountRupee&cu=INR&tn=${Uri.encode(note)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
            setPackage("com.google.android.apps.nbu.paisa.user")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)), "Pay with Google Pay or UPI")
                context.startActivity(chooser)
            } catch (e2: Exception) {
                showToast("No UPI app installed. Copy UPI: $cleanUpi")
            }
        }
    }

    fun addNote(
        title: String,
        content: String,
        noteType: NoteType,
        isPrivate: Boolean,
        dueDateMillis: Long?,
        targetUserId: String? = null
    ) {
        viewModelScope.launch {
            val note = RoomNote(
                id = "note_${UUID.randomUUID().toString().take(8)}",
                roomId = _currentRoomId.value,
                authorUserId = _currentUserId.value,
                title = title.ifBlank { "Notice" },
                content = content,
                noteType = noteType,
                isPrivate = isPrivate,
                targetUserId = targetUserId,
                dueDateMillis = dueDateMillis
            )
            repository.createNote(note)
            try {
                firestoreRepository.saveNote(note)
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore saveNote error: ${e.message}")
            }
            ReminderNotificationHelper.sendReminderNotification(
                context = getApplication(),
                title = "Kaasu Kudu Kaasu: ${note.title}",
                message = note.content.ifBlank { "New note added for room" }
            )
            showToast("Note added successfully")
        }
    }

    fun approveMember(userId: String) {
        val room = uiState.value.currentRoom
        val myId = _currentUserId.value
        val isCreator = room?.adminUid == myId || uiState.value.isCurrentAdmin
        if (!isCreator) {
            showToast("Only the room creator can approve members")
            return
        }
        viewModelScope.launch {
            repository.approveMember(_currentRoomId.value, userId, myId)
            showToast("Member approved!")
        }
    }

    fun removeMember(userId: String) {
        val room = uiState.value.currentRoom
        val myId = _currentUserId.value
        val isCreator = room?.adminUid == myId || uiState.value.isCurrentAdmin
        if (!isCreator) {
            showToast("Only the room creator can remove members")
            return
        }
        viewModelScope.launch {
            repository.removeMember(_currentRoomId.value, userId, myId)
            showToast("Member removed from room")
        }
    }

    fun addFinancialAdjustment(description: String, amountRupees: String) {
        viewModelScope.launch {
            val paise = CurrencyHelper.rupeeToPaise(amountRupees)
            repository.addAuditAdjustment(_currentRoomId.value, _currentUserId.value, description, paise)
            showToast("Financial adjustment recorded in audit log")
        }
    }

    fun updateUserProfile(fullName: String, phone: String, upiId: String, bankAccount: String, ifsc: String) {
        viewModelScope.launch {
            val user = uiState.value.currentUser ?: return@launch
            val updated = user.copy(
                fullName = fullName.ifBlank { user.fullName },
                phone = phone,
                upiId = upiId,
                bankAccountNumber = bankAccount,
                ifscCode = ifsc
            )
            repository.saveUser(updated)
            try {
                firestoreRepository.saveUserProfile(updated)
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore saveUserProfile error: ${e.message}")
            }
            showToast("Profile & UPI information updated successfully")
        }
    }

    fun addMemberByCode(codeOrId: String) {
        val clean = codeOrId.trim()
        if (clean.isBlank()) {
            showToast("Please enter a 6-digit User ID or username")
            return
        }
        val roomId = _currentRoomId.value
        val room = uiState.value.currentRoom
        val myId = _currentUserId.value
        val isCreator = room?.adminUid == myId || uiState.value.isCurrentAdmin
        if (!isCreator) {
            showToast("Only room creator or admin can add members directly")
            return
        }

        viewModelScope.launch {
            val allUsers = repository.getAllUsers().first()
            var targetUser = allUsers.find {
                it.sixDigitUserId == clean || it.id == clean || it.username.equals(clean, ignoreCase = true)
            }
            if (targetUser == null) {
                try {
                    val remote = firestoreRepository.getUserProfile(clean)
                    if (remote != null) {
                        repository.saveUser(remote)
                        targetUser = remote
                    }
                } catch (e: Exception) {
                    Log.e("VwsVM", "Firestore find user error: ${e.message}")
                }
            }

            if (targetUser == null) {
                showToast("No user found with 6-digit ID: $clean")
                return@launch
            }

            val existingMembers = repository.getMembersForRoom(roomId).first()
            if (existingMembers.any { it.userId == targetUser.id }) {
                showToast("${targetUser.fullName} is already in this room")
                return@launch
            }

            if (existingMembers.size >= 6) {
                showToast("Maximum 6 members per room reached")
                return@launch
            }

            val newMember = RoomMember(
                id = "mem_${UUID.randomUUID().toString().take(8)}",
                roomId = roomId,
                userId = targetUser.id,
                role = UserRole.MEMBER,
                status = MemberStatus.ACTIVE,
                joinedAtMillis = System.currentTimeMillis()
            )
            repository.insertRoomMember(newMember)
            try {
                firestoreRepository.addRoomMember(roomId, targetUser.id)
            } catch (e: Exception) {
                Log.e("VwsVM", "Firestore add member error: ${e.message}")
            }
            showToast("Added ${targetUser.fullName} to room!")
        }
    }
}
