package com.example.pam.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object IconHelper {

    private val iconMap: Map<String, ImageVector> = mapOf(
        "AccountBalance" to Icons.Default.AccountBalance,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "Payments" to Icons.Default.Payments,
        "CreditCard" to Icons.Default.CreditCard,
        "Savings" to Icons.Default.Savings,
        "TrendingUp" to Icons.AutoMirrored.Filled.TrendingUp,
        "Restaurant" to Icons.Default.Restaurant,
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "ShoppingBag" to Icons.Default.ShoppingBag,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "ReceiptLong" to Icons.AutoMirrored.Filled.ReceiptLong,
        "Movie" to Icons.Default.Movie,
        "MedicalServices" to Icons.Default.MedicalServices,
        "School" to Icons.Default.School,
        "Flight" to Icons.Default.Flight,
        "Work" to Icons.Default.Work,
        "CardGiftcard" to Icons.Default.CardGiftcard,
        "Home" to Icons.Default.Home,
        "Spa" to Icons.Default.Spa,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "LocalGasStation" to Icons.Default.LocalGasStation,
        "Fastfood" to Icons.Default.Fastfood,
        "Store" to Icons.Default.Store,
        "AttachMoney" to Icons.Default.AttachMoney,
        "CallSplit" to Icons.AutoMirrored.Filled.CallSplit,
        "Category" to Icons.Default.Category
    )

    private val availableIconsList: List<String> = iconMap.keys.toList()

    fun getIcon(name: String): ImageVector {
        return iconMap[name] ?: Icons.Default.Category
    }

    fun getAllAvailableIcons(): List<String> = availableIconsList
}
