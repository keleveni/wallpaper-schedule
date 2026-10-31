package com.keleveni.wallpaperscheduler.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.keleveni.wallpaperscheduler.AppContainer
import com.keleveni.wallpaperscheduler.WallpaperSchedulerApp

val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as WallpaperSchedulerApp).container
