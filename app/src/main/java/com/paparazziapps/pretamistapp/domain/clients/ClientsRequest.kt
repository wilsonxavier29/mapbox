package com.paparazziapps.pretamistapp.domain.clients

import kotlinx.serialization.Serializable

@Serializable
data class ClientsRequest(
    val id: String? = null,
    val document: String?= null,
    val name: String?= null,
    val lastName: String?= null,
    val email: String?= null,
    val phone: String?= null,
    val dateCreated: Long?= null,
    val latitude: String? = null,  // Nueva propiedad para latitud
    val longitude: String? = null  // Nueva propiedad para longitud
) : java.io.Serializable

data class ClientDomain(
    val id: String,
    val document: String,
    val name: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val latitude: String,  // Nueva propiedad para latitud
    val longitude: String  // Nueva propiedad para longitud
)

@Serializable
data class ClientDomainSelect(
    val id: String,
    val document: String,
    val name: String,
    val lastName: String,
    val email: String,
    val phone: String,
    var isSelected: Boolean = false,
    val latitude: String,  // Nueva propiedad para latitud
    val longitude: String  // Nueva propiedad para longitud
) : java.io.Serializable

fun ClientDomain.toClientDomainSelect(): ClientDomainSelect {
    return ClientDomainSelect(
        id = id,
        document = document,
        name = name,
        lastName = lastName,
        email = email,
        phone = phone,
        latitude = latitude,  // Incluye latitud
        longitude = longitude  // Incluye longitud
    )
}

fun List<ClientDomain>.toClientDomainSelect(): List<ClientDomainSelect> {
    return map { it.toClientDomainSelect() }
}

fun ClientsRequest.toClientDomain(): ClientDomain {
    return ClientDomain(
        id = id ?: "",
        document = document ?: "",
        name = name ?: "",
        lastName = lastName ?: "",
        email = email ?: "",
        phone = phone ?: "",
        latitude = latitude ?: "",  // Asigna latitud
        longitude = longitude ?: ""  // Asigna longitud
    )
}

fun ClientDomain.toClientsRequest(): ClientsRequest {
    return ClientsRequest(
        id = id,
        document = document,
        name = name,
        lastName = lastName,
        email = email,
        phone = phone,
        dateCreated = System.currentTimeMillis(),
        latitude = latitude,  // Incluye latitud
        longitude = longitude  // Incluye longitud
    )
}
