package com.scitech.accountex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTemplateDao {
    @Query("SELECT * FROM transaction_templates ORDER BY name")
    fun getAllTemplates(): Flow<List<TransactionTemplate>>

    @Query("SELECT * FROM transaction_templates WHERE id = :id")
    suspend fun getTemplateById(id: Int): TransactionTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TransactionTemplate)

    @Update
    suspend fun updateTemplate(template: TransactionTemplate)

    @Delete
    suspend fun deleteTemplate(template: TransactionTemplate)
}