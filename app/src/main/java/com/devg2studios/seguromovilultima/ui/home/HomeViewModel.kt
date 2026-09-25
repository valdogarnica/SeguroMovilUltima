
package com.devg2studios.seguromovilultima.ui.home

import androidx.lifecycle.ViewModel
import com.devg2studios.seguromovilultima.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun logout() {
        authRepository.logout()
    }
}