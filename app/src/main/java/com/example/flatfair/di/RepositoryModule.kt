package com.example.flatfair.di

import com.example.flatfair.data.repository.ActivityRepositoryImpl
import com.example.flatfair.data.repository.AuthRepositoryImpl
import com.example.flatfair.data.repository.ChoreRepositoryImpl
import com.example.flatfair.data.repository.ExpenseRepositoryImpl
import com.example.flatfair.data.repository.HouseholdRepositoryImpl
import com.example.flatfair.data.repository.ShoppingRepositoryImpl
import com.example.flatfair.data.repository.TransactionRepositoryImpl
import com.example.flatfair.data.repository.UserRepositoryImpl
import com.example.flatfair.domain.repository.ActivityRepository
import com.example.flatfair.domain.repository.AuthRepository
import com.example.flatfair.domain.repository.ChoreRepository
import com.example.flatfair.domain.repository.ExpenseRepository
import com.example.flatfair.domain.repository.HouseholdRepository
import com.example.flatfair.domain.repository.ShoppingRepository
import com.example.flatfair.domain.repository.TransactionRepository
import com.example.flatfair.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChoreRepository(
        choreRepositoryImpl: ChoreRepositoryImpl
    ): ChoreRepository

    @Binds
    @Singleton
    abstract fun bindHouseholdRepository(
        householdRepositoryImpl: HouseholdRepositoryImpl
    ): HouseholdRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepositoryImpl: ExpenseRepositoryImpl
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindActivityRepository(
        activityRepositoryImpl: ActivityRepositoryImpl
    ): ActivityRepository

    @Binds
    @Singleton
    abstract fun bindShoppingRepository(
        shoppingRepositoryImpl: ShoppingRepositoryImpl
    ): ShoppingRepository
}
