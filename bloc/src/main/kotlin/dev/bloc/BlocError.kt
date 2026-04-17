package dev.bloc

/**
 * Generic error type for use with [Cubit.addError] / [Bloc.addError] when no
 * domain-specific error type exists.
 */
class BlocError(message: String, cause: Throwable? = null) : Exception(message, cause)
