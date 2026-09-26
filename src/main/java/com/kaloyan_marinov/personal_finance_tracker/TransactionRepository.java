package com.kaloyan_marinov.personal_finance_tracker;

import org.springframework.data.repository.CrudRepository;

/*
Upon application startup (= At the very beginning of runtime),
the «Spring IoC container» will create an implementation of the following `interface`
and
a «bean definition» based on that implementation will be added to the «Spring IoC container»'s registry of «bean definitions».
That «bean definition» has almost the same name as the class
(the only difference will be a change in the case).
In this case, the name will of the «bean definition» be `transactionRepository`.
 */
public interface TransactionRepository extends CrudRepository<Transaction, Long> {
    
}
