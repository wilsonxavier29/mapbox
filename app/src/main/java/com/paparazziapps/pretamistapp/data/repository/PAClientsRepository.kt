package com.paparazziapps.pretamistapp.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.paparazziapps.pretamistapp.data.network.PAResult
import com.paparazziapps.pretamistapp.domain.clients.ClientDomain

interface PAClientsRepository {
    suspend fun createClient(clientDomain: ClientDomain): PAResult<Void>
    suspend fun searchClientByEmailV2(email: String): PAResult<DocumentSnapshot>
    suspend fun searchByClientName(name: String): PAResult<List<ClientDomain>>
    suspend fun getClientsOnlyFirst20(): PAResult<List<ClientDomain>>
}