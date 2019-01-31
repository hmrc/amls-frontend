/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.PropertyChecks
import play.api.mvc.Call
import utils.{AmlsSpec, AuthorisedFixture}
import play.api.test.Helpers._
import org.scalacheck.Gen
import uk.gov.hmrc.http.cache.client.CacheMap
import models.responsiblepeople.ResponsiblePerson.flowFromDeclaration
import play.api.inject.guice.GuiceApplicationBuilder

import scala.annotation.tailrec
import scala.concurrent.Future

class ResponsiblePersonAddControllerSpec extends AmlsSpec
  with MustMatchers with MockitoSugar with ScalaFutures with PropertyChecks {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ResponsiblePeopleAddController (
      dataCacheConnector = mock[DataCacheConnector],
      authConnector = self.authConnector
    )

    @tailrec
    final def buildTestSequence(requiredCount: Int, acc: Seq[ResponsiblePerson] = Nil): Seq[ResponsiblePerson] = {
      require(requiredCount >= 0, "cannot build a sequence with negative elements")
      if (requiredCount == acc.size) {
        acc
      } else {
        buildTestSequence(requiredCount, acc :+ ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false))))
      }
    }

    def guidanceOptions(currentCount: Int) = Table(
      ("guidanceRequested", "fromDeclaration", "expectedRedirect"),
      (true, Some(`flowFromDeclaration`), controllers.responsiblepeople.routes.WhatYouNeedController.get(currentCount + 1, Some(`flowFromDeclaration`))),
      (true, None, controllers.responsiblepeople.routes.WhoMustRegisterController.get(currentCount + 1)),
      (false, None, controllers.responsiblepeople.routes.WhatYouNeedController.get(currentCount + 1))
    )
  }

  "ResponsiblePeopleController" when {
    "get is called" should {
      "add empty bankdetails and redirect to the correct page" in new Fixture {
        val min = 0
        val max = 25
        val requiredSuccess =10


        val zeroCase = Gen.const(0)
        val emptyCache = CacheMap("", Map.empty)
        val reasonableCounts = for (n <- Gen.choose(min, max)) yield n
        val partitions = Seq (zeroCase, reasonableCounts)

        forAll(reasonableCounts, minSuccessful(requiredSuccess)) { currentCount: Int =>
          forAll(guidanceOptions(currentCount)) { (guidanceRequested: Boolean, fromDeclaration: Option[String], expectedRedirect: Call) =>
            val testSeq  = buildTestSequence(currentCount)

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(testSeq)))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val resultF = controller.get(guidanceRequested, fromDeclaration)(request)

            status(resultF) must be(SEE_OTHER)
            redirectLocation(resultF) must be(Some(expectedRedirect.url))

            verify(controller.dataCacheConnector)
              .save[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key), meq(testSeq :+ ResponsiblePerson()))(any(), any(), any())

            reset(controller.dataCacheConnector)
          }
        }
      }
    }
  }
}
