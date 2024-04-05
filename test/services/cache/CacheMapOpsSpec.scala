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
import models.businessmatching.BusinessMatching
import org.scalatest.OptionValues
import play.api.libs.json.{JsArray, JsBoolean, JsString, Json}
import services.cache.CacheMapOps.RichCacheMap
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec

class CacheMapOpsSpec extends AmlsSpec with OptionValues {

  val doubleEncryptedUnsanitaryAmp = "'" + "avIcAP8AvFcC96lrOKiAAcSQu9oB2IuZtI4dWP/ucDEkPPLTUVxhYmH4INDPogXoIHRuqBmY+FR9zkPRZsiXBdR6dJRY3FEoUV6+y8zfUytTuMXc2RZ0mJx4MuqmZfNmO97UsFsnDpp+YcsJUGIei7Ci6WatX5MMgOMwPbFUOhRemp8mvaXto7guCYeRWyfkfH5nI+r8LF2UvNPZIsxeIZhyV5XNJyiQjzf9Wmw8G2Wx82nqVCMyZeH5j/O9fifBuIbcbpJrPV2Ua+FQlcYi3fymLcdCvi1AX5vbzLJjJBbLXMtRYvI7W7/O/FjoyaOGDb7q63+iVz5GGZnG40syuqVyMDbCUS8zmtvDVeiRfC7kEMY5zQ//hBFTrXNiIWH+RvZmI4nEty4KsrEOX7vZBK/SQcBivgHFZZoCz3zIQ+fKc5P2y6Vk4Nsv4fn+OapA" + "'"

  val encryptedBusinessMatching = "qAlez0NHNr7roTpl+rKXA0dxEyPDvBD74UHddr1l5UeUwg5bzZ3YCGI2f5uY06Ns/87fk2ZBLsHv7TaJchdp5IQQF8GUpfbM0HoX2un6d6vrSRIWZy1ZCxkTn9XNmH4A6gb+6XqjeTo1hEo4k9ol4UGLJibLZjg7PWYw/ORnvpdAVhoblkGjqe40yAX8gn52W+9Eo05tDjKT6eyVTAi2KmnusQGZsUw0VORHz8mVoU2waeAUxt9KAro6W1gqm3tLWjGtLq9SDbNR+YAJSYgHfuT3XLfNEQcnCHx2wkFTGwv3Nd2dUTzuNN7CujO2bznCUfIM6mJXiwpu8nS4anIkwLSMpqwCgtKVM6Q4eahVJIOWZKHd0B7aH5vEzqoblQegObjXWdbx2rBzXc9A67hTB+5pzj9rWTklhW1A+CcwG6gaiSZELNqHkDUUepwTlICwpvMhmnPIOjIQPjugL/pDdXWvH8Ylg+GI+fVn6py3jjqGywffOneS1/wJbft9rwx2Kn9VskUf44ButUK0TXGXoXZvF46//bTjuJ6P+bnLLiR3LsNSvfR3GOLz7N720PVGpRFxSTV6CF5TnWQ3sSvQKQXgUPdq/erfDlXexiyRPj/IjN41WQ/9XBFOK0394FtJH2WyV1wc9D1Z9lpG+vOFmVdanDJZyjrn8pMcoX01J+BJ54nqnw/fBCH4hvKwdoKhajaU/6ULQkEsyH6hSRgp179gU/ReYhlPMd3ROGRMJgwwfal/mnXDXgJod4xRP8pv9+EbN/SWfAAVVNdvvzrLKp+XccSXUaaJ/sgBEDIiejz6hGTUzvzVVsnTbFEg5iiqFBmEezYjPD/g9r/f/37s1+3Ks5YzH0YwysrZt3BdOTY="


  ".sanitiseDoubleDecrypt" must {
    val ampData = Json.obj(
      "typeOfParticipant" -> JsArray(Seq(JsString("artGalleryOwner"), JsString("artDealer"), JsString("artAuctioneer"), JsString("somethingElse"))),
      "typeOfParticipantDetail" -> JsString("Art surveying"),
      "soldOverThreshold" -> JsBoolean(true),
      "dateTransactionOverThreshold" -> JsString("2020-01-10"),
      "identifyLinkedTransactions" -> JsBoolean(true),
      "percentageExpectedTurnover" -> JsString("twentyOneToForty")
    )
    val expectedAmp = Amp(ampData, false, true)

    "decrypt once successfully" in {
      // Given
      val cacheMap = CacheMap("test-cache-map-1", Map(BusinessMatching.key -> JsString(encryptedBusinessMatching)))

      // When
      val unencryptedBusinessMatching = cacheMap.sanitiseDoubleDecrypt(BusinessMatching.key)(BusinessMatching.reads, compositeSymmetricCrypto)

      // Then
      unencryptedBusinessMatching.value mustEqual BusinessMatching(None,None,None,None,None,None,false,false,false)
    }

    "remove random single quotes from the beginning and end & decrypt twice successfully" in {
      // Given
      val cacheMap = CacheMap("test-cache-map-1", Map(Amp.key -> JsString(doubleEncryptedUnsanitaryAmp)))

      // When
      val unencryptedAmp: Option[Amp] = cacheMap.sanitiseDoubleDecrypt(Amp.key)(Amp.reads, compositeSymmetricCrypto)

      // Then
      unencryptedAmp.value mustEqual expectedAmp
    }

    "decrypt sanitary encrypted strings" in {
      // Given
      val sanitaryDoubleEncryptedAmp = doubleEncryptedUnsanitaryAmp.stripPrefix("'").stripSuffix("'")
      val cacheMap = CacheMap("test-cache-map-1", Map("amp" -> JsString(sanitaryDoubleEncryptedAmp)))

      // When
      val unencryptedAmp: Option[Amp] = cacheMap.sanitiseDoubleDecrypt(Amp.key)(Amp.reads, compositeSymmetricCrypto)

      // Then
      unencryptedAmp.value mustEqual expectedAmp
    }
  }
}
