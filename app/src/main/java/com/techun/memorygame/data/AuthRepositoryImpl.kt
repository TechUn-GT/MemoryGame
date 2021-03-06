package com.techun.memorygame.data

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.techun.memorygame.di.FirebaseModule
import com.techun.memorygame.domain.model.UserModel
import com.techun.memorygame.domain.repository.AuthRepository
import com.techun.memorygame.utils.Constants.IMAGE_USER_DEFAULT
import com.techun.memorygame.utils.Constants.INFO_NOT_SET
import com.techun.memorygame.utils.Constants.USER_LOGGED_IN_ID
import com.techun.memorygame.utils.Constants.USER_NAME
import com.techun.memorygame.utils.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    @FirebaseModule.UsersCollection private val usersCollection: CollectionReference
) : AuthRepository {
    override suspend fun login(email: String, password: String): Flow<DataState<Boolean>> = flow {
        emit(DataState.Loading)
        try {
            var isSuccessful = false
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { isSuccessful = true }
                .addOnFailureListener { isSuccessful = false }
                .await()
            emit(DataState.Success(isSuccessful))
            emit(DataState.Finished)
        } catch (e: Exception) {
            emit(DataState.Error(e))
            emit(DataState.Finished)
        }
    }

    override suspend fun loginWithGoogle(acct: GoogleSignInAccount): Flow<DataState<UserModel>> =
        flow {
            emit(DataState.Loading)
            try {
                lateinit var exception: Exception
                var registeredUser = UserModel()
                val credential: AuthCredential =
                    GoogleAuthProvider.getCredential(acct.idToken, null)
                auth.signInWithCredential(credential).addOnSuccessListener { task ->
                    if (task != null) {
                        val firebaseUser: FirebaseUser = task.user!!
                        registeredUser = UserModel(
                            id = firebaseUser.uid,
                            email = firebaseUser.email!!,
                            imageUrl = firebaseUser.photoUrl.toString(),
                            userName = firebaseUser.displayName!!
                        )
                    } else {
                        exception = Exception("N/A")
                    }
                }.await()
                if (registeredUser.id != INFO_NOT_SET) {
                    emit(DataState.Success(registeredUser))
                    emit(DataState.Finished)
                } else {
                    emit(DataState.Error(exception))
                    emit(DataState.Finished)
                }
            } catch (e: Exception) {
                emit(DataState.Error(e))
                emit(DataState.Finished)
            }
        }

    override suspend fun signUp(user: UserModel, password: String): Flow<DataState<UserModel>> =
        flow {
            emit(DataState.Loading)
            try {
                lateinit var exception: Exception
                var registeredUser = UserModel()
                auth.createUserWithEmailAndPassword(user.email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser: FirebaseUser = task.result!!.user!!
                            registeredUser = UserModel(
                                id = firebaseUser.uid,
                                email = user.email
                            )
                        } else {
                            exception = task.exception!!
                        }
                    }.await()
                if (registeredUser.id != INFO_NOT_SET) {
                    emit(DataState.Success(registeredUser))
                    emit(DataState.Finished)
                } else {
                    emit(DataState.Error(exception))
                    emit(DataState.Finished)
                }
            } catch (e: Exception) {
                emit(DataState.Error(e))
                emit(DataState.Finished)
            }
        }

    override suspend fun logOut(): Flow<DataState<Boolean>> = flow {
        emit(DataState.Loading)
        try {
            auth.signOut()
            emit(DataState.Success(true))
            emit(DataState.Finished)
        } catch (e: Exception) {
            emit(DataState.Error(e))
            emit(DataState.Finished)
        }
    }

    override suspend fun getUserData(): Flow<DataState<Boolean>> = flow {
        var requestStatus = false
        val currentUser = auth.currentUser
        emit(DataState.Loading)
        try {
            currentUser?.uid?.let {
                usersCollection.document(it)
                    .get()
                    .addOnSuccessListener { document ->
                        val user = document.toObject(UserModel::class.java)!!
                        requestStatus = true
                        USER_LOGGED_IN_ID = user.id
                        USER_NAME = user.userName
                        IMAGE_USER_DEFAULT = user.imageUrl
                    }
                    .addOnFailureListener { requestStatus = false }.await()
            }
            emit(DataState.Success(requestStatus))
            emit(DataState.Finished)
        } catch (e: Exception) {
            emit(DataState.Error(e))
            emit(DataState.Finished)
        }
    }

    override suspend fun saveUser(user: UserModel): Flow<DataState<Boolean>> = flow {
        emit(DataState.Loading)
        try {
            var uploadSuccessful = false
            usersCollection.document(user.id).set(user, SetOptions.merge())
                .addOnSuccessListener {
                    uploadSuccessful = true
                }.addOnFailureListener {
                    uploadSuccessful = false
                }.await()
            emit(DataState.Success(uploadSuccessful))
            emit(DataState.Finished)
        } catch (e: Exception) {
            emit(DataState.Error(e))
            emit(DataState.Finished)
        }
    }

    override suspend fun userExist(documentId: String): Flow<DataState<Boolean>> = flow {
        emit(DataState.Loading)
        try {
            var uploadSuccessful = false
            usersCollection.document(documentId).get()
                .addOnSuccessListener { document ->
                    uploadSuccessful = document.get("email") != null
                }
                .addOnFailureListener {
                    uploadSuccessful = false
                }.await()
            emit(DataState.Success(uploadSuccessful))
            emit(DataState.Finished)
        } catch (e: Exception) {
            emit(DataState.Error(e))
            emit(DataState.Finished)
        }
    }

    override suspend fun verifyPasswordReset(email: String): Flow<DataState<Boolean>> = flow {
        emit(DataState.Loading)
        try {
            val uploadSuccessful = true
            auth.sendPasswordResetEmail(email).await()
            emit(DataState.Success(uploadSuccessful))
            emit(DataState.Finished)
        } catch (e: Exception) {
            emit(DataState.Error(e))
            emit(DataState.Finished)
        }
    }
}