package com.scitech.accountex.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// 🎨 PRIMITIVE PALETTE (RAW INK)
// ============================================================================

// --- NEBULA THEME (Identity) ---
private val NebulaTeal        = Color(0xFF00897B)
private val NebulaPurple      = Color(0xFFAA83B9)
private val NebulaCyan        = Color(0xFF26C6DA)
private val NebulaGold        = Color(0xFFFFD700)
private val NebulaCoral       = Color(0xFFFF7043)

// --- CYBERPUNK THEME ---
private val CyberNeonPink     = Color(0xFFFF00FF)
private val CyberElectricBlue = Color(0xFF00F3FF)
private val CyberAcidGreen    = Color(0xFFCCFF00)
private val CyberDeepVoid     = Color(0xFF050510)
private val CyberGridGray     = Color(0xFF1A1A2E)

// --- NATURE THEME ---
private val NatureForest      = Color(0xFF2E7D32)
private val NatureEarth       = Color(0xFF5D4037)
private val NatureLeaf        = Color(0xFFAED581)
private val NatureSand        = Color(0xFFFFF3E0)
private val NatureStone       = Color(0xFF455A64)

// --- CRIMSON BLADE (Power) ---
private val CrimsonRed        = Color(0xFFD50000)
private val CrimsonDark       = Color(0xFF1B0000)
private val CrimsonCharcoal   = Color(0xFF212121)
private val CrimsonWhite      = Color(0xFFFFEBEE)

// --- ROYAL GOLD (Luxury) ---
private val RoyalIndigo       = Color(0xFF1A237E)
private val RoyalGold         = Color(0xFFFFD700)
private val RoyalCream        = Color(0xFFFFF8E1)
private val RoyalNight        = Color(0xFF0D1236)

// --- ARCTIC (Clean) ---
private val ArcticBlue        = Color(0xFF00B0FF)
private val ArcticIce         = Color(0xFFE1F5FE)
private val ArcticWhite       = Color(0xFFFFFFFF)
private val ArcticSteel       = Color(0xFF455A64)

// --- NEUTRALS ---
private val VoidBlack         = Color(0xFF0A0A0A)
private val Charcoal          = Color(0xFF121212)
private val SteelGray         = Color(0xFF37474F)
private val CloudWhite        = Color(0xFFFFFFFF)
private val MistGray          = Color(0xFFF5F7FA)

// --- FINANCIAL INDICATORS ---
private val ProfitGreen       = Color(0xFF00C853)
private val LossRed           = Color(0xFFD50000)

// ============================================================================
// 🧠 SEMANTIC MODEL
// ============================================================================

data class AccountexColors(
    val brandPrimary: Color,
    val brandSecondary: Color,
    val actionDominant: Color,
    val actionSubtle: Color,

    val background: Color,
    val surfaceCard: Color,
    val surfaceHighlight: Color,
    val divider: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textInverse: Color,
    val textOnBrand: Color,

    val income: Color,
    val expense: Color,
    val trendUp: Color,
    val trendDown: Color,

    val currencyTier1: Color,
    val currencyTier2: Color,
    val currencyTier3: Color,
    val currencyTier4: Color,
    val currencyTier5: Color,
    val currencyCoin: Color,

    val headerGradient: Brush,
    val primaryGradient: Brush,

    val isDark: Boolean
)

// ============================================================================
// 🌈 THEME DEFINITIONS
// ============================================================================

// --- 1. NEBULA (DEFAULT) ---
val NebulaLightPalette = AccountexColors(
    brandPrimary = NebulaTeal, brandSecondary = NebulaPurple, actionDominant = NebulaGold, actionSubtle = NebulaTeal.copy(alpha = 0.1f),
    background = CloudWhite, surfaceCard = MistGray, surfaceHighlight = Color(0xFFEDE7F6), divider = Color(0xFFE0E0E0),
    textPrimary = SteelGray, textSecondary = Color(0xFF78909C), textInverse = CloudWhite, textOnBrand = CloudWhite,
    income = NebulaTeal, expense = LossRed, trendUp = ProfitGreen, trendDown = LossRed,
    currencyTier1 = Color(0xFF455A64), currencyTier2 = Color(0xFFFF9800), currencyTier3 = Color(0xFF7E57C2), currencyTier4 = Color(0xFF00ACC1), currencyTier5 = Color(0xFF8BC34A), currencyCoin = Color(0xFFFFD700),
    headerGradient = Brush.verticalGradient(listOf(NebulaTeal, NebulaPurple)),
    primaryGradient = Brush.horizontalGradient(listOf(NebulaTeal, NebulaCyan)),
    isDark = false
)

val NebulaDarkPalette = AccountexColors(
    brandPrimary = NebulaCyan, brandSecondary = NebulaPurple, actionDominant = NebulaGold, actionSubtle = NebulaCyan.copy(alpha = 0.2f),
    background = VoidBlack, surfaceCard = Charcoal, surfaceHighlight = Color(0xFF1E1E2E), divider = Color(0xFF2C2C2C),
    textPrimary = Color(0xFFECEFF1), textSecondary = Color(0xFFB0BEC5), textInverse = VoidBlack, textOnBrand = CloudWhite,
    income = Color(0xFF69F0AE), expense = Color(0xFFFF5252), trendUp = Color(0xFF00E676), trendDown = Color(0xFFFF1744),
    currencyTier1 = Color(0xFF90A4AE), currencyTier2 = Color(0xFFFFB74D), currencyTier3 = Color(0xFFB39DDB), currencyTier4 = Color(0xFF4DD0E1), currencyTier5 = Color(0xFFAED581), currencyCoin = Color(0xFFFFE082),
    headerGradient = Brush.verticalGradient(listOf(Color(0xFF00695C), Color(0xFF6A1B9A))),
    primaryGradient = Brush.horizontalGradient(listOf(NebulaTeal, NebulaPurple)),
    isDark = true
)

// --- 2. CYBERPUNK (HIGH CONTRAST) ---
val CyberpunkPalette = AccountexColors(
    brandPrimary = CyberNeonPink, brandSecondary = CyberElectricBlue, actionDominant = CyberAcidGreen, actionSubtle = CyberNeonPink.copy(0.2f),
    background = CyberDeepVoid, surfaceCard = CyberGridGray, surfaceHighlight = Color(0xFF252540), divider = CyberElectricBlue.copy(0.3f),
    textPrimary = Color(0xFFE0E0FF), textSecondary = CyberElectricBlue.copy(0.7f), textInverse = CyberDeepVoid, textOnBrand = Color.White,
    income = CyberAcidGreen, expense = CyberNeonPink, trendUp = CyberAcidGreen, trendDown = CyberNeonPink,
    currencyTier1 = Color(0xFF00FFFF), currencyTier2 = Color(0xFFFF00FF), currencyTier3 = Color(0xFFFFFF00), currencyTier4 = Color(0xFF00FF00), currencyTier5 = Color(0xFFFFA500), currencyCoin = Color(0xFFE0E0E0),
    headerGradient = Brush.verticalGradient(listOf(Color(0xFF2B0045), Color(0xFF001545))),
    primaryGradient = Brush.horizontalGradient(listOf(CyberNeonPink, CyberElectricBlue)),
    isDark = true
)

// --- 3. NATURE (CALM) ---
val NatureLightPalette = AccountexColors(
    brandPrimary = NatureForest, brandSecondary = NatureEarth, actionDominant = NatureEarth, actionSubtle = NatureLeaf.copy(0.3f),
    background = NatureSand, surfaceCard = CloudWhite, surfaceHighlight = Color(0xFFDCEDC8), divider = Color(0xFFBCAAA4),
    textPrimary = Color(0xFF33691E), textSecondary = NatureStone, textInverse = CloudWhite, textOnBrand = CloudWhite,
    income = NatureForest, expense = Color(0xFFBF360C), trendUp = NatureForest, trendDown = Color(0xFFBF360C),
    currencyTier1 = NatureStone, currencyTier2 = NatureEarth, currencyTier3 = NatureForest, currencyTier4 = NatureLeaf, currencyTier5 = Color(0xFF8D6E63), currencyCoin = Color(0xFFFFB300),
    headerGradient = Brush.verticalGradient(listOf(NatureForest, NatureLeaf)),
    primaryGradient = Brush.horizontalGradient(listOf(NatureForest, NatureEarth)),
    isDark = false
)

// --- 4. CRIMSON BLADE (POWER) ---
val CrimsonPalette = AccountexColors(
    brandPrimary = CrimsonRed, brandSecondary = Color.Black, actionDominant = Color.White, actionSubtle = CrimsonRed.copy(0.2f),
    background = CrimsonDark, surfaceCard = CrimsonCharcoal, surfaceHighlight = Color(0xFF3E0000), divider = Color(0xFF424242),
    textPrimary = CrimsonWhite, textSecondary = Color(0xFFB0BEC5), textInverse = Color.Black, textOnBrand = Color.White,
    income = Color(0xFF00E676), expense = CrimsonRed, trendUp = Color(0xFF00E676), trendDown = CrimsonRed,
    currencyTier1 = Color(0xFFEF9A9A), currencyTier2 = Color(0xFFFFCC80), currencyTier3 = Color(0xFFCE93D8), currencyTier4 = Color(0xFF80DEEA), currencyTier5 = Color(0xFFC5E1A5), currencyCoin = Color(0xFFFFD54F),
    headerGradient = Brush.verticalGradient(listOf(Color(0xFFB71C1C), Color(0xFF212121))),
    primaryGradient = Brush.horizontalGradient(listOf(CrimsonRed, Color.Black)),
    isDark = true
)

// --- 5. ROYAL GOLD (LUXURY) ---
val RoyalPalette = AccountexColors(
    brandPrimary = RoyalGold, brandSecondary = RoyalIndigo, actionDominant = RoyalGold, actionSubtle = RoyalGold.copy(0.2f),
    background = RoyalNight, surfaceCard = Color(0xFF121630), surfaceHighlight = Color(0xFF202650), divider = RoyalGold.copy(0.2f),
    textPrimary = RoyalCream, textSecondary = Color(0xFF9FA8DA), textInverse = RoyalNight, textOnBrand = RoyalCream,
    income = RoyalGold, expense = Color(0xFFFF5252), trendUp = RoyalGold, trendDown = Color(0xFFFF5252),
    currencyTier1 = RoyalGold, currencyTier2 = RoyalGold.copy(0.8f), currencyTier3 = RoyalIndigo.copy(alpha=0.6f), currencyTier4 = Color(0xFF7986CB), currencyTier5 = Color(0xFF3949AB), currencyCoin = RoyalGold,
    headerGradient = Brush.verticalGradient(listOf(RoyalIndigo, Color(0xFF000051))),
    primaryGradient = Brush.horizontalGradient(listOf(RoyalGold, RoyalIndigo)),
    isDark = true
)

// --- 6. ARCTIC (CLEAN) ---
val ArcticPalette = AccountexColors(
    brandPrimary = ArcticBlue, brandSecondary = ArcticSteel, actionDominant = ArcticBlue, actionSubtle = ArcticBlue.copy(0.1f),
    background = ArcticWhite, surfaceCard = ArcticIce, surfaceHighlight = Color(0xFFB3E5FC), divider = Color(0xFFB0BEC5),
    textPrimary = ArcticSteel, textSecondary = Color(0xFF78909C), textInverse = Color.White, textOnBrand = Color.White,
    income = ArcticBlue, expense = Color(0xFFE53935), trendUp = ArcticBlue, trendDown = Color(0xFFE53935),
    currencyTier1 = ArcticSteel, currencyTier2 = Color(0xFF29B6F6), currencyTier3 = Color(0xFF5C6BC0), currencyTier4 = Color(0xFF26A69A), currencyTier5 = Color(0xFF66BB6A), currencyCoin = Color(0xFFFFCA28),
    headerGradient = Brush.verticalGradient(listOf(ArcticBlue, ArcticSteel)),
    primaryGradient = Brush.horizontalGradient(listOf(ArcticBlue, Color(0xFF81D4FA))),
    isDark = false
)