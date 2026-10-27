package com.example.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.UserEntity
import com.example.data.VwsDatabase
import com.example.data.VwsFirestoreRepository
import com.example.model.User
import com.example.model.UserRole
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "SignInScreen"

@Composable
fun SignInScreen(
    onSignInSuccess: (userId: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Admin Popup Window State
    var showAdminDialog by remember { mutableStateOf(false) }
    var adminUsername by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var adminErrorMessage by remember { mutableStateOf<String?>(null) }
    var isAdminLoading by remember { mutableStateOf(false) }

    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutVwsDialog(onDismiss = { showAboutDialog = false })
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FAFB)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Login Navigation Bar with Application Icon, App Info, and Admin Portal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Application Icon + Brand Name + App Info button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showAboutDialog = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("login_nav_app_info")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_vws_logo),
                        contentDescription = "Application Icon",
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "VWS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFF111827)
                    )
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "App Info",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "App Info",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                }

                // Right: Admin portal link with 10px font size
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .clickable {
                            adminErrorMessage = null
                            showAdminDialog = true
                        }
                        .testTag("top_right_admin_portal_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Admin Portal",
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Admin Portal",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Brand Logo
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                shadowElevation = 3.dp,
                modifier = Modifier.size(88.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_vws_logo),
                        contentDescription = "VWS Logo",
                        modifier = Modifier.size(62.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "VWS Workspace",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
            )

            Text(
                text = "Bachelor Room Expense Splitter & Settlement",
                fontSize = 13.sp,
                color = Color(0xFF4B5563),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Error banner
            if (errorMessage != null) {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFF991B1B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Success banner
            if (successMessage != null) {
                Surface(
                    color = Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = successMessage!!,
                            color = Color(0xFF065F46),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Google Sign-In Card for Roommates / Members
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Roommate & Member Sign In",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }

                    Text(
                        text = "Sign in directly with your Google account to access your room, track split expenses, and record peer settlements.",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563),
                        lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    val webClientId = context.getString(R.string.default_web_client_id)
                                    val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(context, request)
                                    val credential = result.credential
                                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val idToken = googleIdTokenCredential.idToken
                                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                                        val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                                        val firebaseUser = authResult.user
                                        val uid = firebaseUser?.uid ?: "user_google"

                                        withContext(Dispatchers.IO) {
                                            val dao = VwsDatabase.getDatabase(context).vwsDao()
                                            val email = firebaseUser?.email ?: ""
                                            val displayName = firebaseUser?.displayName ?: email.substringBefore("@").ifBlank { "Roommate" }
                                            val user = User(
                                                id = uid,
                                                username = email.substringBefore("@").ifBlank { "user_${uid.take(6)}" },
                                                fullName = displayName,
                                                email = email,
                                                role = UserRole.MEMBER,
                                                isApproved = true
                                            )
                                            dao.insertUser(UserEntity.fromDomain(user))
                                            try {
                                                VwsFirestoreRepository(context).saveUserProfile(user)
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Save profile error: ${e.message}")
                                            }
                                        }

                                        isLoading = false
                                        onSignInSuccess(uid)
                                    } else {
                                        isLoading = false
                                        errorMessage = "Unexpected credential format returned"
                                    }
                                } catch (e: GetCredentialCancellationException) {
                                    isLoading = false
                                    Log.d(TAG, "User cancelled Google Sign-In")
                                } catch (e: Exception) {
                                    isLoading = false
                                    val raw = e.localizedMessage ?: e.message ?: ""
                                    errorMessage = if (raw.contains("No credential", ignoreCase = true) || raw.contains("16:", ignoreCase = true)) {
                                        "No Google account found on device. Sign in to Google Play on your device or access via Admin Portal (top right)."
                                    } else {
                                        "Google Sign-In failed: $raw"
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_google_signin"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Continue with Google",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Popup Window (Dialog) for Admin Login
        if (showAdminDialog) {
            Dialog(
                onDismissRequest = { if (!isAdminLoading) showAdminDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("admin_portal_dialog"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Dialog Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFECFDF5),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_vws_logo),
                                            contentDescription = "Application Logo",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Admin Portal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                            }
                            IconButton(onClick = { showAdminDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF6B7280))
                            }
                        }

                        Text(
                            text = "Sign in using administrator credentials to configure rooms, approve members, and oversee financials.",
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 18.sp
                        )

                        // Error Banner inside Dialog
                        if (adminErrorMessage != null) {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = adminErrorMessage!!,
                                    color = Color(0xFF991B1B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = adminUsername,
                            onValueChange = { adminUsername = it },
                            label = { Text("Username") },
                            placeholder = { Text("Enter admin username") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF6B7280))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                focusedLabelColor = Color(0xFF059669),
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827)
                            )
                        )

                        OutlinedTextField(
                            value = adminPassword,
                            onValueChange = { adminPassword = it },
                            label = { Text("Password") },
                            placeholder = { Text("Enter admin password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6B7280))
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Password Visibility",
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                focusedLabelColor = Color(0xFF059669),
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827)
                            )
                        )

                        Button(
                            onClick = {
                                val user = adminUsername.trim()
                                val pass = adminPassword.trim()
                                if (user.isBlank() || pass.isBlank()) {
                                    adminErrorMessage = "Please enter both admin username and password."
                                    return@Button
                                }
                                if (user != "admin" || pass != "2124") {
                                    adminErrorMessage = "Invalid admin credentials. Please try again."
                                    return@Button
                                }

                                isAdminLoading = true
                                adminErrorMessage = null
                                coroutineScope.launch {
                                    val adminUid = "admin_master_vws"
                                    withContext(Dispatchers.IO) {
                                        val dao = VwsDatabase.getDatabase(context).vwsDao()
                                        val admin = User(
                                            id = adminUid,
                                            username = user.ifBlank { "admin" },
                                            fullName = "VWS Admin",
                                            email = "admin@vwsapp.com",
                                            phone = "+91 9876543210",
                                            upiId = "vwsadmin@upi",
                                            role = UserRole.ADMIN,
                                            isApproved = true
                                        )
                                        dao.insertUser(UserEntity.fromDomain(admin))
                                        try {
                                            VwsFirestoreRepository(context).saveUserProfile(admin)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Save admin profile error: ${e.message}")
                                        }
                                    }
                                    isAdminLoading = false
                                    showAdminDialog = false
                                    onSignInSuccess(adminUid)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_submit_login"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                            enabled = !isAdminLoading
                        ) {
                            if (isAdminLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "Sign In as Admin",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
