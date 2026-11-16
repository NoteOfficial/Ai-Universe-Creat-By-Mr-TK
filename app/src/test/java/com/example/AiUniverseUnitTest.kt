package com.example

import com.example.data.model.AiRegistry
import com.example.data.network.OriginCheckStatus
import com.example.data.network.UrlValidator
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.stringsFor
import com.example.ui.theme.ColorPaletteTheme
import com.example.ui.theme.paletteFor
import com.example.ui.tools.CookieParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiUniverseUnitTest {

  @Test
  fun `build config matches requested package id and version code 1201`() {
    assertEquals("com.tkofficial.aiuniverse", BuildConfig.APPLICATION_ID)
    assertEquals(1201, BuildConfig.VERSION_CODE)
    assertEquals("2.1", BuildConfig.VERSION_NAME)
  }

  @Test
  fun `registry has 29 unique official AI platforms with verified https urls`() {
    val platforms = AiRegistry.officialPlatforms
    assertEquals(29, platforms.size)
    assertEquals(29, platforms.map { it.id }.distinct().size)
    assertEquals(29, platforms.map { it.officialUrl }.distinct().size)
    assertTrue(platforms.all { UrlValidator.isValidHttpsUrl(it.officialUrl) })

    assertEquals("https://lmarena.ai/", AiRegistry.findById("arena_ai")?.officialUrl)
    assertEquals("https://chatgpt.com/", AiRegistry.findById("chatgpt")?.officialUrl)
    assertEquals("https://gemini.google.com/", AiRegistry.findById("gemini")?.officialUrl)
    assertEquals("https://claude.ai/", AiRegistry.findById("claude")?.officialUrl)
    assertEquals("https://chat.deepseek.com/", AiRegistry.findById("deepseek")?.officialUrl)
    assertEquals("https://v0.dev/", AiRegistry.findById("v0")?.officialUrl)
    assertEquals("https://aistudio.google.com/", AiRegistry.findById("google_ai_studio")?.officialUrl)
  }

  @Test
  fun `origin validator detects official vs auth flow vs unverified external hosts`() {
    val arena = AiRegistry.findById("arena_ai")!!
    assertEquals(
      OriginCheckStatus.APPROVED_OFFICIAL,
      UrlValidator.checkOrigin("https://lmarena.ai/leaderboard", arena.approvedOrigins),
    )
    assertEquals(
      OriginCheckStatus.APPROVED_AUTH_FLOW,
      UrlValidator.checkOrigin("https://accounts.google.com/signin", arena.approvedOrigins),
    )
    assertEquals(
      OriginCheckStatus.UNVERIFIED_EXTERNAL,
      UrlValidator.checkOrigin("https://unrelated-domain.example.com/", arena.approvedOrigins),
    )
  }

  @Test
  fun `five color palettes provide distinct vibrant primary accents for light and dark`() {
    assertEquals(5, ColorPaletteTheme.entries.size)
    val lightAccents =
      ColorPaletteTheme.entries.map { paletteFor(it, isDark = false).accent }
    val darkAccents =
      ColorPaletteTheme.entries.map { paletteFor(it, isDark = true).accent }
    assertEquals(5, lightAccents.distinct().size)
    assertEquals(5, darkAccents.distinct().size)
  }

  @Test
  fun `english and bangla localization cover tutorial pages themes and key ui labels`() {
    val en = stringsFor(AppLanguage.ENGLISH)
    val bn = stringsFor(AppLanguage.BANGLA)

    assertEquals(8, en.tutorialPages.size)
    assertEquals(8, bn.tutorialPages.size)
    assertTrue(en.tutorialPages.all { it.title.isNotBlank() && it.description.isNotBlank() })
    assertTrue(bn.tutorialPages.all { it.title.isNotBlank() && it.description.isNotBlank() })

    assertEquals("Home", en.navHome)
    assertEquals("হোম", bn.navHome)
    assertEquals("সেটিংস", bn.settings)
    assertFalse(bn.colorTheme == en.colorTheme)
    assertFalse(bn.paletteCosmicViolet == en.paletteCosmicViolet)
    assertFalse(bn.cookies.manageCookiesTitle == en.cookies.manageCookiesTitle)
  }

  @Test
  fun `cookie parser handles raw header, set-cookie lines, and key-value pairs`() {
    val raw = CookieParser.parseRawHeader("session=abc123; theme=dark; cf_clearance=xyz")
    assertEquals(3, raw.size)
    assertEquals("session", raw[0].name)
    assertEquals("abc123", raw[0].value)

    val setCookie =
      CookieParser.parseImportText(
        """
        Set-Cookie: __Secure-next-auth.session-token=tok999; Path=/; Secure; HttpOnly
        token=val456; lang=bn
        """.trimIndent()
      )
    assertEquals(3, setCookie.size)
    assertEquals("__Secure-next-auth.session-token", setCookie[0].name)
    assertTrue(setCookie[0].httpOnly)
    assertEquals("token", setCookie[1].name)
    assertEquals("bn", setCookie[2].value)

    val netscape =
      CookieParser.parseImportText(
        ".chatgpt.com\tTRUE\t/\tTRUE\t1893456000\toai_sid\tsecret77"
      )
    assertEquals(1, netscape.size)
    assertEquals("oai_sid", netscape[0].name)
    assertEquals("secret77", netscape[0].value)
    assertEquals(".chatgpt.com", netscape[0].domain)
  }
}
