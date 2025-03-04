/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.cache

import models.amp.Amp
import models.tradingpremises.TradingPremises
import org.scalatest.OptionValues
import play.api.libs.json.{JsArray, JsBoolean, JsObject, JsString}
import utils.AmlsSpec

class CacheOpsSpec extends AmlsSpec with OptionValues {

  object TestCacheClient extends CacheOps

  val encryptedTradingPremises =
    "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"

  val doubleEncryptedAmp =
    "eNMq8gbH7h1r/oKEz3Fl1GCzr3zgqgqYSCtaFIVD0evtBDfbtyE5+V7G+22KKpjtGM3CL/tUGZgWF+Xm6bw9QInlcuq9bxEAXHq7Ao/0ymYDUHB9ao2nlwxlRYfLjRTXYKxP2Xe+faMXnEpCXAg/d9vnXncQf//fzaXxSS+xcI6Fi2FWDZ2+XuiTWQPtKFIWA30CDJer9aFJcGa3YaoOEsTZ+TI6Gu5H2381OKujumZdXAC33AYNHlMTzDgoQKW8hs/iXI6TxYq82L1xRj8Zpb+jnf2lY9n+6Um6us2mNEUAEzerjg6FYa0VbOhsdcno1XCbX9C9ffGeTvOuK+NC5CjVBWWP+ldW6vizplPTdXmMb9phzWHlQ9F/QaRUi2f2SBiEdJkfq75RtuPXss5rFFsteok+H79FQbEKsc3MZ0VWqAd8pC5pcJHUks6e+GqlC0Dz5U4Lu/H3nF3Tt4RFsbQBZkoalXJuBK1X+G9v5gniQIOqcq325Ky+cyoguAu5rYrtBLng//yUMcar6vgr6G61nESuL6vzoZquaMPDgUuRCjoHa4BSvBU1uwt4NEIkdwzj5VuvAwqqQ84TcdxTuTInrXHND6d6tDIAP6is7gI="

  ".catchDoubleEncryption" must {
    "decrypt double encrypted value" in {
      // Given
      val cache       = Cache("test-cache", Map("amp" -> JsString(doubleEncryptedAmp)))
      val expectedAmp = Amp(
        JsObject(
          Seq(
            (
              "typeOfParticipant",
              JsArray(
                Seq(
                  JsString("artGalleryOwner"),
                  JsString("artDealer"),
                  JsString("artAuctioneer"),
                  JsString("somethingElse")
                )
              )
            ),
            ("typeOfParticipantDetail", JsString("Art surveying")),
            ("soldOverThreshold", JsBoolean(true)),
            ("dateTransactionOverThreshold", JsString("2020-01-10")),
            ("identifyLinkedTransactions", JsBoolean(true)),
            ("percentageExpectedTurnover", JsString("twentyOneToForty"))
          )
        ),
        false,
        true
      )

      // When
      val optAmp = TestCacheClient.catchDoubleEncryption(cache, "amp")(Amp.reads, compositeSymmetricCrypto)

      // Then
      optAmp.value mustEqual expectedAmp
    }

    "decrypt single encrypted value" in {
      // Given
      val cache = Cache("test-cache", Map("trading-premises" -> JsString(encryptedTradingPremises)))

      // When
      val optTradingPremises = TestCacheClient
        .catchDoubleEncryption(cache, "trading-premises")(TradingPremises.reads, compositeSymmetricCrypto)

      // Then
      optTradingPremises.value mustEqual TradingPremises()
    }

    "throw exception when random value" in {
      // Given
      val cache = Cache("test-cache", Map("trading-premises" -> JsString("hfepw3i9u84y2r97ufghrewyegwoy")))

      // when
      val expectedEx = intercept[SecurityException] {
        TestCacheClient
          .catchDoubleEncryption(cache, "trading-premises")(TradingPremises.reads, compositeSymmetricCrypto)
      }

      // Then
      expectedEx.getMessage mustEqual "Unable to decrypt value"
    }
  }

}
