package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class VwsFirestoreRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    private fun getEffectiveUid(fallback: String? = null): String {
        return auth.currentUser?.uid ?: fallback ?: "admin_master_vws"
    }

    suspend fun saveUserProfile(user: User) {
        val effectiveUid = user.id.ifBlank { getEffectiveUid() }
        val docRef = firestore.collection("users").document(effectiveUid)
        val data = mapOf(
            "uid" to effectiveUid,
            "fullName" to user.fullName,
            "username" to user.username,
            "email" to user.email,
            "phone" to user.phone,
            "upiId" to user.upiId,
            "sixDigitUserId" to user.sixDigitUserId,
            "isAdmin" to (user.role == UserRole.ADMIN),
            "updatedAt" to System.currentTimeMillis().toString()
        )
        try {
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun createRoom(room: Room, creatorUid: String = "") {
        val effectiveAdmin = if (creatorUid.isNotBlank()) creatorUid else room.adminUid.ifBlank { getEffectiveUid() }
        val docRef = firestore.collection("rooms").document(room.id)
        val data = mapOf(
            "id" to room.id,
            "name" to room.name,
            "address" to room.address,
            "ebConsumerNumber" to room.ebConsumerNumber,
            "waterTaxNumber" to room.waterTaxNumber,
            "ownerName" to room.ownerName,
            "ownerPhone" to room.ownerPhone,
            "monthlyRentPaise" to room.monthlyRentPaise,
            "rentDueDay" to room.rentDueDay,
            "description" to room.description,
            "code" to room.invitationCode,
            "adminUid" to effectiveAdmin,
            "memberUids" to listOf(effectiveAdmin),
            "currency" to "INR",
            "createdAt" to System.currentTimeMillis().toString()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    suspend fun deleteRoom(roomId: String) {
        val docRef = firestore.collection("rooms").document(roomId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    suspend fun getRoomByCode(code: String): Room? {
        val collection = firestore.collection("rooms")
        return try {
            val snapshot = collection.whereEqualTo("code", code).limit(1).get().await()
            val doc = snapshot.documents.firstOrNull() ?: return null
            val data = doc.data ?: return null
            Room(
                id = doc.id,
                name = data["name"] as? String ?: "Bachelor Room",
                address = data["address"] as? String ?: "",
                ebConsumerNumber = data["ebConsumerNumber"] as? String ?: "",
                waterTaxNumber = data["waterTaxNumber"] as? String ?: "",
                ownerName = data["ownerName"] as? String ?: "",
                ownerPhone = data["ownerPhone"] as? String ?: "",
                monthlyRentPaise = (data["monthlyRentPaise"] as? Number)?.toLong() ?: 0L,
                rentDueDay = (data["rentDueDay"] as? Number)?.toInt() ?: 5,
                description = data["description"] as? String ?: "",
                adminUid = data["adminUid"] as? String ?: "",
                invitationCode = code
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, collection.path)
            null
        }
    }

    suspend fun addRoomMember(roomId: String, userId: String) {
        val docRef = firestore.collection("rooms").document(roomId)
        try {
            val doc = docRef.get().await()
            val existing = (doc.get("memberUids") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            if (!existing.contains(userId)) {
                docRef.update("memberUids", existing + userId).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun addExpense(expense: Expense, participants: List<String>) {
        val effectivePayer = expense.payerUserId.ifBlank { getEffectiveUid() }
        val docRef = firestore.collection("expenses").document(expense.id)
        val data = mapOf(
            "id" to expense.id,
            "roomId" to expense.roomId,
            "title" to expense.description,
            "amountPaise" to expense.totalAmountPaise,
            "paidByUid" to effectivePayer,
            "paidByName" to expense.payerUserId,
            "splitType" to expense.splitType.name,
            "participantUids" to participants,
            "dateMillis" to expense.expenseDateMillis,
            "notes" to expense.notes,
            "createdAt" to System.currentTimeMillis().toString()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    suspend fun recordTransaction(payment: PaymentRecord) {
        val effectiveFrom = payment.fromUserId.ifBlank { getEffectiveUid() }
        val docRef = firestore.collection("transactions").document(payment.id)
        val data = mapOf(
            "id" to payment.id,
            "roomId" to payment.roomId,
            "expenseId" to (payment.relatedExpenseId ?: ""),
            "fromUid" to effectiveFrom,
            "fromName" to payment.fromUserId,
            "toUid" to payment.toUserId,
            "toName" to payment.toUserId,
            "amountPaise" to payment.amountPaise,
            "status" to payment.status.name,
            "method" to payment.paymentMethod.name,
            "upiDeepLink" to payment.referenceId,
            "createdAt" to System.currentTimeMillis().toString()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    suspend fun getUserProfile(uid: String): User? {
        val docRef = firestore.collection("users").document(uid)
        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            val fullName = data["fullName"] as? String ?: "User"
            val username = data["username"] as? String ?: uid
            val email = data["email"] as? String ?: ""
            val phone = data["phone"] as? String ?: ""
            val upiId = data["upiId"] as? String ?: ""
            val sixDigitUserId = data["sixDigitUserId"] as? String ?: ""
            val isAdmin = data["isAdmin"] as? Boolean ?: false
            User(
                id = uid,
                username = username,
                fullName = fullName,
                email = email,
                phone = phone,
                upiId = upiId,
                role = if (isAdmin) UserRole.ADMIN else UserRole.MEMBER,
                isApproved = true
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            null
        }
    }

    suspend fun getRoomsForUser(uid: String): List<Room> {
        val collection = firestore.collection("rooms")
        return try {
            val snapshot = collection.get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val id = doc.id
                val name = data["name"] as? String ?: "Room"
                val adminUid = data["adminUid"] as? String ?: ""
                val memberUids = (data["memberUids"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val code = data["code"] as? String ?: ""
                val address = data["address"] as? String ?: ""
                val ebConsumerNumber = data["ebConsumerNumber"] as? String ?: ""
                val waterTaxNumber = data["waterTaxNumber"] as? String ?: ""
                val ownerName = data["ownerName"] as? String ?: ""
                val ownerPhone = data["ownerPhone"] as? String ?: ""
                val monthlyRentPaise = (data["monthlyRentPaise"] as? Number)?.toLong() ?: 0L
                val rentDueDay = (data["rentDueDay"] as? Number)?.toInt() ?: 5
                val description = data["description"] as? String ?: ""
                if (uid.isBlank() || adminUid == uid || memberUids.contains(uid)) {
                    Room(
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
                        invitationCode = code
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, collection.path)
            emptyList()
        }
    }

    fun observeRooms(): Flow<List<Map<String, Any>>> = callbackFlow {
        val collection = firestore.collection("rooms")
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collection.path)
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    fun observeExpenses(roomId: String? = null): Flow<List<Map<String, Any>>> = callbackFlow {
        val collection = firestore.collection("expenses")
        val query = if (roomId.isNullOrBlank()) collection else collection.whereEqualTo("roomId", roomId)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collection.path)
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    fun observeTransactions(roomId: String? = null): Flow<List<Map<String, Any>>> = callbackFlow {
        val collection = firestore.collection("transactions")
        val query = if (roomId.isNullOrBlank()) collection else collection.whereEqualTo("roomId", roomId)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collection.path)
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    fun observeUsers(): Flow<List<Map<String, Any>>> = callbackFlow {
        val collection = firestore.collection("users")
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, collection.path)
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    fun observeNotes(): Flow<List<Map<String, Any>>> = callbackFlow {
        val registration = firestore.collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("VwsFirestore", "observeNotes error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveNote(note: RoomNote) {
        val docRef = firestore.collection("notes").document(note.id)
        val data = mapOf(
            "id" to note.id,
            "roomId" to note.roomId,
            "authorUserId" to note.authorUserId,
            "targetUserId" to (note.targetUserId ?: ""),
            "title" to note.title,
            "content" to note.content,
            "noteType" to note.noteType.name,
            "isPrivate" to note.isPrivate,
            "dueDateMillis" to (note.dueDateMillis ?: 0L),
            "createdAt" to note.createdAtMillis.toString()
        )
        try {
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }
}
