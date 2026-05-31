package com.example.resiliencesandbox

import org.junit.Test
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Message

class EngineTest {
    @Test
    fun testReflection() {
        println("--- CONVERSATION METHODS ---")
        Conversation::class.java.methods.forEach {
            println("${it.name} - ${it.returnType.name}")
        }
    }
}
