package com.example.data

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {
    val allSessions: Flow<List<CounterSession>> = sessionDao.getAllSessions()

    suspend fun insertSession(session: CounterSession): Int {
        return sessionDao.insertSession(session).toInt()
    }

    suspend fun updateSession(session: CounterSession) {
        sessionDao.updateSession(session)
    }

    suspend fun getSessionById(id: Int): CounterSession? {
        return sessionDao.getSessionById(id)
    }
}
