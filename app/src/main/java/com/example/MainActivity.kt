package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.VwsBottomNavigation
import com.example.ui.components.VwsTopAppBar
import com.example.ui.screens.*
import com.example.model.CurrencyHelper
import com.example.model.PaymentMethod
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.ReminderNotificationHelper
import com.example.viewmodel.AppTab
import com.example.viewmodel.VwsViewModel

import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val viewModel: VwsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderNotificationHelper.initNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        ReminderNotificationHelper.initNotificationChannel(context)
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val auth = remember { FirebaseAuth.getInstance() }
                var authUser by remember { mutableStateOf(auth.currentUser) }
                var localUserId by remember { mutableStateOf<String?>(null) }

                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        authUser = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose {
                        auth.removeAuthStateListener(listener)
                    }
                }

                LaunchedEffect(authUser, localUserId) {
                    if (authUser != null) {
                        viewModel.syncUserWithAuth(authUser!!)
                    } else if (localUserId != null) {
                        viewModel.syncLocalUser(localUserId!!)
                    }
                }

                val isUserSignedIn = authUser != null || localUserId != null

                if (!isUserSignedIn) {
                    SignInScreen(
                        onSignInSuccess = { uid ->
                            val effectiveId = uid ?: "admin_master_vws"
                            localUserId = effectiveId
                            viewModel.syncLocalUser(effectiveId)
                        }
                    )
                } else {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val context = LocalContext.current

                    var showAddNoteDialog by remember { mutableStateOf(false) }
                    var showAboutDialog by remember { mutableStateOf(false) }

                    // Observe toast notifications
                    LaunchedEffect(uiState.toastMessage) {
                        uiState.toastMessage?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearToast()
                        }
                    }

                    Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        VwsTopAppBar(
                            selectedRoom = uiState.currentRoom,
                            allRooms = uiState.rooms,
                            onSelectRoom = { viewModel.switchRoom(it) },
                            onCreateNewRoom = { viewModel.openCreateRoom(true) },
                            onJoinRoom = { viewModel.openJoinRoom(true) },
                            onOpenCalendar = { viewModel.openCalendarReport(true) },
                            onOpenAbout = { showAboutDialog = true },
                            onOpenAdmin = { viewModel.openAdminDashboard(true) },
                            onOpenProfile = { viewModel.openProfile(true) },
                            isAdmin = uiState.isCurrentAdmin
                        )
                    },
                    bottomBar = {
                        VwsBottomNavigation(
                            currentTab = uiState.currentTab,
                            onTabSelected = { viewModel.setTab(it) },
                            unreadNotesCount = uiState.unreadNotesCount
                        )
                    },
                    floatingActionButton = {
                        if (uiState.currentTab == AppTab.HOME) {
                            ExtendedFloatingActionButton(
                                onClick = { viewModel.openAddExpense(true) },
                                containerColor = EmeraldPrimary,
                                contentColor = androidx.compose.ui.graphics.Color.White,
                                icon = { Icon(Icons.Default.Add, contentDescription = "Add Expense") },
                                text = { Text("Add Expense") },
                                modifier = Modifier.testTag("fab_add_expense")
                            )
                        } else if (uiState.currentTab == AppTab.NOTES) {
                            FloatingActionButton(
                                onClick = { showAddNoteDialog = true },
                                containerColor = EmeraldPrimary,
                                contentColor = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.testTag("fab_add_note")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Note")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.currentTab) {
                            AppTab.HOME -> {
                                HomeScreen(
                                    uiState = uiState,
                                    onAddExpenseClick = { viewModel.openAddExpense(true) },
                                    onRecordPaymentClick = { viewModel.openRecordPayment(true) },
                                    onCalendarClick = { viewModel.openCalendarReport(true) },
                                    onAddNoteClick = { showAddNoteDialog = true },
                                    onExpenseClick = { viewModel.setExpenseDetail(it) },
                                    onCreateRoomClick = { viewModel.openCreateRoom(true) },
                                    onJoinRoomClick = { viewModel.openJoinRoom(true) },
                                    onDeleteRoomClick = { viewModel.deleteRoom(it) },
                                    onAddMemberByCode = { viewModel.addMemberByCode(it) },
                                    onDirectPayGooglePay = { payee, amountPaise, note ->
                                        viewModel.launchGooglePay(
                                            context = this@MainActivity,
                                            payeeUpi = payee.upiId,
                                            payeeName = payee.fullName.ifBlank { payee.username },
                                            amountPaise = amountPaise,
                                            note = note
                                        )
                                    },
                                    onDirectPayCash = { payee, amountPaise ->
                                        viewModel.submitPayment(
                                            toUserId = payee.id,
                                            amountRupees = CurrencyHelper.formatPaiseToPlainRupee(amountPaise),
                                            method = PaymentMethod.CASH,
                                            referenceId = "CASH-SETTLE-${System.currentTimeMillis() % 100000}",
                                            notes = "Instant cash settlement from Home Quick Pay"
                                        )
                                    }
                                )
                            }
                            AppTab.TRANSACTIONS -> {
                                TransactionsScreen(
                                    uiState = uiState,
                                    onLaunchUpi = { payeeUpi, payeeName, amountPaise, note ->
                                        viewModel.launchUpiPayment(context, payeeUpi, payeeName, amountPaise, note)
                                    },
                                    onVerifyPayment = { paymentId, newStatus, note ->
                                        viewModel.verifyPayment(paymentId, newStatus, note)
                                    },
                                    onRecordNewPayment = { viewModel.openRecordPayment(true) }
                                )
                            }
                            AppTab.NOTES -> {
                                NotesScreen(
                                    uiState = uiState,
                                    onAddNoteClick = { showAddNoteDialog = true }
                                )
                            }
                        }

                        // Modal Dialogs
                        if (uiState.isAddExpenseOpen) {
                            AddExpenseDialog(
                                uiState = uiState,
                                onDismiss = { viewModel.openAddExpense(false) },
                                onSubmit = { desc, amount, cat, customCat, splitType, payer, date, members, inputs, notes ->
                                    viewModel.addExpense(desc, amount, cat, customCat, splitType, payer, date, members, inputs, notes)
                                }
                            )
                        }

                        if (uiState.isRecordPaymentOpen) {
                            RecordPaymentDialog(
                                uiState = uiState,
                                onDismiss = { viewModel.openRecordPayment(false) },
                                onSubmit = { toUser, amount, method, ref, notes ->
                                    viewModel.submitPayment(toUser, amount, method, ref, notes)
                                }
                            )
                        }

                        uiState.activeExpenseDetail?.let { expense ->
                            ExpenseDetailDialog(
                                expense = expense,
                                uiState = uiState,
                                onDismiss = { viewModel.setExpenseDetail(null) },
                                onDirectPayGooglePay = { payee, amountPaise, note ->
                                    viewModel.launchGooglePay(
                                        context = context,
                                        payeeUpi = payee.upiId.ifBlank { "vwsroom@upi" },
                                        payeeName = payee.fullName,
                                        amountPaise = amountPaise,
                                        note = note
                                    )
                                },
                                onDirectPayCash = { payeeId, amountPaise, splitId ->
                                    viewModel.submitPayment(
                                        toUserId = payeeId,
                                        amountRupees = com.example.model.CurrencyHelper.formatPaiseToPlainRupee(amountPaise),
                                        method = com.example.model.PaymentMethod.CASH,
                                        referenceId = "CASH_${splitId.take(6)}",
                                        notes = "Direct cash payment for ${expense.description}"
                                    )
                                    viewModel.setExpenseDetail(null)
                                }
                            )
                        }

                        if (uiState.isCalendarReportOpen) {
                            CalendarReportDialog(
                                uiState = uiState,
                                onDismiss = { viewModel.openCalendarReport(false) }
                            )
                        }

                        if (uiState.isAdminDashboardOpen) {
                            AdminDashboardDialog(
                                uiState = uiState,
                                onDismiss = { viewModel.openAdminDashboard(false) },
                                onApproveMember = { viewModel.approveMember(it) },
                                onRemoveMember = { viewModel.removeMember(it) },
                                onAddAdjustment = { desc, amount ->
                                    viewModel.addFinancialAdjustment(desc, amount)
                                },
                                onDeleteRoom = { viewModel.deleteRoom(it) },
                                onAddMemberByCode = { viewModel.addMemberByCode(it) }
                            )
                        }

                        if (uiState.isCreateRoomOpen) {
                            CreateRoomDialog(
                                uiState = uiState,
                                onDismiss = { viewModel.openCreateRoom(false) },
                                onSubmit = { name, addr, eb, water, oName, oPhone, rent, due, desc, members ->
                                    viewModel.createRoom(name, addr, eb, water, oName, oPhone, rent, due, desc, members)
                                }
                            )
                        }

                        if (uiState.isJoinRoomOpen) {
                            JoinRoomDialog(
                                onDismiss = { viewModel.openJoinRoom(false) },
                                onJoin = { code ->
                                    viewModel.joinRoomByCode(code)
                                }
                            )
                        }

                        if (uiState.isProfileOpen) {
                            ProfileDialog(
                                uiState = uiState,
                                onDismiss = { viewModel.openProfile(false) },
                                onSaveProfile = { name, phone, upi, bank, ifsc ->
                                    viewModel.updateUserProfile(name, phone, upi, bank, ifsc)
                                },
                                onSwitchUser = { viewModel.switchUser(it) },
                                onSignOut = {
                                    viewModel.signOut {
                                        localUserId = null
                                    }
                                }
                            )
                        }

                        if (showAddNoteDialog) {
                            AddNoteDialog(
                                roommates = uiState.users,
                                currentUserId = uiState.currentUserId,
                                onDismiss = { showAddNoteDialog = false },
                                onSubmit = { title, content, type, isPrivate, due, targetUser ->
                                    viewModel.addNote(title, content, type, isPrivate, due, targetUser)
                                    showAddNoteDialog = false
                                }
                            )
                        }

                        if (showAboutDialog) {
                            AboutVwsDialog(
                                onDismiss = { showAboutDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
