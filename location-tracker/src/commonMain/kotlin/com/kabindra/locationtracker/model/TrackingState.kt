package com.kabindra.locationtracker.model

sealed interface TrackingState {
    data object Idle : TrackingState
    data object Starting : TrackingState
    data object Running : TrackingState
    data object Stopped : TrackingState
    data object PermissionDenied : TrackingState
    data object LocationServicesDisabled : TrackingState
    data class Error(val message: String) : TrackingState
}
