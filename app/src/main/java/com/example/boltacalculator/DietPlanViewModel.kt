package com.example.boltacalculator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boltacalculator.Global.gemini_reply
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DietPlanViewModel : ViewModel() {
    private val openAIService = OpenAIService()

    private val _dietPlanState = MutableStateFlow<UiState>(UiState.Initial)
    val dietPlanState: StateFlow<UiState> = _dietPlanState

    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        data class Success(val dietPlan: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    //todo diet plan
    fun fetchDietPlan(age: Int, weight: Double, height: Double, gender: String, activity: String, goal: String, preference: String) {
        val messaage = " Create a personalized diet plan for a $age-year-old $gender weighing $weight kg and $height cm tall.\n" +
                "                Activity level: $activity\n" +
                "                Goal: $goal\n" +
                "                Diet preferences: $preference\n" +
                "                Provide a comprehensive diet plan including:\n" +
                "                1. Daily calorie recommendation\n" +
                "                2. Meal breakdown (Breakfast, Lunch, Dinner)\n" +
                "                3. Snack suggestions\n" +
                "                4. Nutritional guidelines\n" +
                "                5. Hydration recommendations"
        viewModelScope.launch {
            _dietPlanState.value = UiState.Loading

            openAIService.generateContent(messaage)
                .onSuccess { dietPlan ->
                    _dietPlanState.value = UiState.Success(dietPlan)
                    gemini_reply = dietPlan
                    Log.d("checkviewmodel", "onSuccess: $dietPlan")
                }
                .onFailure { error ->
                    _dietPlanState.value = UiState.Error(error.message ?: "Unknown error")
                    Log.d("checkviewmodel", "onFailure: ${error.message}")
                }
        }
    }
}

