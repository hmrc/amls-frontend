/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import connectors.DataCacheConnector
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Minute, Span}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.{ExecutionContext, Future}

class AuditEventsControllerSpec  extends AmlsSpec with MockitoSugar with ScalaFutures {
  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    val auditConnector = mock[AuditConnector]

    lazy val defaultBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[AuditConnector].to(auditConnector))

    val builder: GuiceApplicationBuilder = defaultBuilder
    lazy val app: Application = builder.build()
    lazy val controller: AuditEventsController = app.injector.instanceOf[AuditEventsController]
    //val auditConnector: AuditConnector = app.injector.instanceOf[AuditConnector]
    implicit val ec: ExecutionContext = mock[ExecutionContext]
  }

  "AuditEventsController" must {
    "sends a timeout audit even via audit connector" in new Fixture {

      when {
        auditConnector.sendEvent(any())(any(), any())
      } thenReturn Future.successful(Success)


      whenReady(controller.sendAuditEvent()(request), timeout(Span(1, Minute))) { _ =>
        verify(auditConnector, times(1)).sendEvent(any())(any(), any())
      }
    }
  }
}
