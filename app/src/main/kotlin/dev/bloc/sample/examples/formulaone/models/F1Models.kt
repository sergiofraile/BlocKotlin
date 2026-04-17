package dev.bloc.sample.examples.formulaone.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DriversChampionshipResponse(
    @SerialName("drivers_championship") val driversChampionship: List<DriverChampionship>,
)

@Serializable
data class DriverChampionship(
    val classificationId: Int,
    val position: Int,
    val points: Int,
    val wins: Int,
    val driver: Driver,
    val team: Team,
)

@Serializable
data class Driver(
    val name: String,
    val surname: String,
    val nationality: String,
    val birthday: String,
    val number: Int,
    val shortName: String,
)

@Serializable
data class Team(
    val teamName: String,
    val country: String,
)
