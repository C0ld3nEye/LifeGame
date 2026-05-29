package com.example.resiliencesandbox

import com.example.resiliencesandbox.narrative.util.JsonCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class JsonCleanerTest {

    @Test
    fun testCleanAndParse_perfectJson() {
        val rawInput = """{"action_type":"aggression","target":"boss_martin","intensity":8}"""
        val result = JsonCleaner.cleanAndParse(rawInput)
        
        assertNotNull(result)
        assertEquals("aggression", result.action_type)
        assertEquals("boss_martin", result.target)
        assertEquals(8, result.intensity)
    }

    @Test
    fun testCleanAndParse_withChatter() {
        val rawInput = """
            Bien sûr, voici le résultat de mon analyse :
            {
                "action_type": "discussion",
                "target": "friend_lucas",
                "intensity": 5
            }
            En espérant que cela vous convienne !
        """.trimIndent()
        
        val result = JsonCleaner.cleanAndParse(rawInput)
        
        assertNotNull(result)
        assertEquals("discussion", result.action_type)
        assertEquals("friend_lucas", result.target)
        assertEquals(5, result.intensity)
    }

    @Test
    fun testCleanAndParse_invalidJsonFallback() {
        val rawInput = "Désolé, je ne peux pas faire cela car l'action est trop vague."
        val result = JsonCleaner.cleanAndParse(rawInput)
        
        assertNotNull(result)
        assertEquals("discussion", result.action_type)
        assertEquals("self", result.target)
        assertEquals(3, result.intensity)
    }
}
