package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.ui.verification.VerificationPlan
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class VerificationPlanRepository private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("frndom_verification_plans_prefs", Context.MODE_PRIVATE)

    private val dbRef: DatabaseReference? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                com.example.util.FirebaseDatabaseHelper.getInstance().getReference("verification_packages")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("VerificationPlanRepo", "FirebaseDatabase not initialized: ${e.message}")
            null
        }
    }

    private val _plansFlow = MutableStateFlow<List<VerificationPlan>>(loadLocalPlans())
    val plansFlow: StateFlow<List<VerificationPlan>> = _plansFlow.asStateFlow()

    init {
        listenToFirebasePackages()
    }

    private fun getDefaultPlans(): List<VerificationPlan> {
        return listOf(
            VerificationPlan(
                id = "plan_free_tier",
                title = "Free Badge Pass",
                durationDays = 7,
                durationText = "7 Days Free Validity",
                price = 0.0,
                tag = "FREE",
                description = "Apply instantly for free verification badge without wallet balance",
                isFree = true,
                createdAt = 1000L
            ),
            VerificationPlan(
                id = "plan_1_month",
                title = "1 Month",
                durationDays = 30,
                durationText = "30 Days Validity",
                price = 99.0,
                tag = "Starter",
                description = "Perfect to try out green badge benefits & trust",
                isFree = false,
                createdAt = 2000L
            ),
            VerificationPlan(
                id = "plan_6_months",
                title = "6 Months",
                durationDays = 180,
                durationText = "180 Days Validity",
                price = 499.0,
                tag = "Popular • Save 16%",
                description = "Great value for active creators & sellers",
                isFree = false,
                createdAt = 3000L
            ),
            VerificationPlan(
                id = "plan_12_months",
                title = "12 Months (1 Year)",
                durationDays = 365,
                durationText = "365 Days Validity",
                price = 999.0,
                tag = "Best Value • Save 20%",
                description = "Maximum savings with full year peace of mind",
                isFree = false,
                createdAt = 4000L
            )
        )
    }

    private fun loadLocalPlans(): List<VerificationPlan> {
        val json = prefs.getString("saved_plans_json", null)
        if (json.isNullOrBlank()) {
            val defaults = getDefaultPlans()
            saveLocalPlans(defaults)
            return defaults
        }

        val list = mutableListOf<VerificationPlan>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val price = obj.optDouble("price", 0.0)
                val isFree = obj.optBoolean("isFree", price <= 0.0)
                val currency = obj.optString("currency", "BDT")
                list.add(
                    VerificationPlan(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Verification Package"),
                        durationDays = obj.optInt("durationDays", 30),
                        durationText = obj.optString("durationText", "${obj.optInt("durationDays", 30)} Days Validity"),
                        price = price,
                        currency = currency,
                        tag = if (obj.has("tag") && !obj.isNull("tag") && obj.optString("tag").isNotBlank()) obj.optString("tag") else null,
                        description = obj.optString("description", "Exclusive green verification badge benefits"),
                        isFree = isFree,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("VerificationPlanRepo", "Error parsing cached plans: ${e.message}")
        }

        return if (list.isNotEmpty()) list.sortedBy { it.createdAt } else getDefaultPlans()
    }

    private fun saveLocalPlans(list: List<VerificationPlan>) {
        try {
            val arr = JSONArray()
            for (p in list) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("title", p.title)
                    put("durationDays", p.durationDays)
                    put("durationText", p.durationText)
                    put("price", p.price)
                    put("currency", p.currency)
                    if (p.tag != null) put("tag", p.tag)
                    put("description", p.description)
                    put("isFree", p.isFree || p.price <= 0.0)
                    put("createdAt", p.createdAt)
                }
                arr.put(obj)
            }
            prefs.edit().putString("saved_plans_json", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("VerificationPlanRepo", "Error saving plans: ${e.message}")
        }
    }

    private fun listenToFirebasePackages() {
        try {
            dbRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || !snapshot.hasChildren()) {
                        // If no packages on Firebase yet, upload defaults
                        val current = _plansFlow.value.ifEmpty { getDefaultPlans() }
                        current.forEach { plan ->
                            dbRef?.child(plan.id)?.setValue(plan.toMap())
                        }
                        return
                    }

                    val list = mutableListOf<VerificationPlan>()
                    for (child in snapshot.children) {
                        try {
                            val id = child.child("id").getValue(String::class.java) ?: child.key ?: UUID.randomUUID().toString()
                            val title = child.child("title").getValue(String::class.java) ?: "Package"
                            val durationDays = (child.child("durationDays").getValue(Long::class.java) ?: child.child("durationDays").getValue(Int::class.java)?.toLong() ?: 30L).toInt()
                            val durationText = child.child("durationText").getValue(String::class.java) ?: "$durationDays Days Validity"
                            val price = child.child("price").getValue(Double::class.java)
                                ?: child.child("price").getValue(Long::class.java)?.toDouble()
                                ?: 0.0
                            val currency = child.child("currency").getValue(String::class.java) ?: "BDT"
                            val tag = child.child("tag").getValue(String::class.java)
                            val description = child.child("description").getValue(String::class.java) ?: ""
                            val isFree = child.child("isFree").getValue(Boolean::class.java) ?: (price <= 0.0)
                            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            list.add(
                                VerificationPlan(
                                    id = id,
                                    title = title,
                                    durationDays = durationDays,
                                    durationText = durationText,
                                    price = price,
                                    currency = currency,
                                    tag = tag,
                                    description = description,
                                    isFree = isFree,
                                    createdAt = createdAt
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("VerificationPlanRepo", "Error reading child plan: ${e.message}")
                        }
                    }

                    if (list.isNotEmpty()) {
                        val sorted = list.sortedBy { it.createdAt }
                        _plansFlow.value = sorted
                        saveLocalPlans(sorted)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("VerificationPlanRepo", "Firebase packages cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("VerificationPlanRepo", "Error listening to packages: ${e.message}")
        }
    }

    fun addOrUpdatePlan(plan: VerificationPlan) {
        val newId = if (plan.id.isBlank()) "plan_${UUID.randomUUID().toString().take(8)}" else plan.id
        val finalPlan = plan.copy(
            id = newId,
            isFree = plan.isFree || plan.price <= 0.0,
            durationText = if (plan.durationText.isBlank()) "${plan.durationDays} Days Validity" else plan.durationText,
            createdAt = if (plan.createdAt <= 0L) System.currentTimeMillis() else plan.createdAt
        )

        val current = _plansFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == finalPlan.id }
        if (index >= 0) {
            current[index] = finalPlan
        } else {
            current.add(finalPlan)
        }

        _plansFlow.value = current
        saveLocalPlans(current)

        try {
            dbRef?.child(finalPlan.id)?.setValue(finalPlan.toMap())
        } catch (e: Exception) {
            Log.e("VerificationPlanRepo", "Firebase save plan error: ${e.message}")
        }
    }

    fun deletePlan(planId: String) {
        val current = _plansFlow.value.toMutableList()
        current.removeAll { it.id == planId }
        _plansFlow.value = current
        saveLocalPlans(current)

        try {
            dbRef?.child(planId)?.removeValue()
        } catch (e: Exception) {
            Log.e("VerificationPlanRepo", "Firebase delete plan error: ${e.message}")
        }
    }

    fun resetToDefaults() {
        val defaults = getDefaultPlans()
        _plansFlow.value = defaults
        saveLocalPlans(defaults)
        try {
            dbRef?.removeValue()
            defaults.forEach { p ->
                dbRef?.child(p.id)?.setValue(p.toMap())
            }
        } catch (e: Exception) {
            Log.e("VerificationPlanRepo", "Firebase reset error: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: VerificationPlanRepository? = null

        fun getInstance(context: Context): VerificationPlanRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VerificationPlanRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
