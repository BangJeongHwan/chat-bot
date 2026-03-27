package com.stockbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StockbotApplication

fun main(args: Array<String>) {
    runApplication<StockbotApplication>(*args)
}
