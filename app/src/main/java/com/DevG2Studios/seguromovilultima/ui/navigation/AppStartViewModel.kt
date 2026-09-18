package com.DevG2Studios.seguromovilultima.ui.navigation

import androidx.lifecycle.ViewModel
import com.DevG2Studios.seguromovilultima.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
}