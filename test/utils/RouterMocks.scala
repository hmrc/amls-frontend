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

package utils

import models.flowmanagement.PageId
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.flowmanagement.Router

import scala.concurrent.Future

trait RouterMocks extends MockitoSugar {
  implicit def createRouter[T](implicit m: Manifest[Router[T]]) = {
    val r = mock[Router[T]]
    r.mockRoute
    r
  }

  val defaultResult = Future.successful(Redirect("/"))

  implicit class RouterMocking[T](router: Router[T]) {
    def mockRoute(credId: String, pageId: PageId, model: T, edit: Boolean = false, returnValue: Future[Result] = defaultResult) =
      when(router.getRouteNewAuth(credId, eqTo(pageId), eqTo(model), eqTo(edit))(any(), any())) thenReturn returnValue

    def mockRoute(returnValue: Future[Result]) =
      when(router.getRouteNewAuth(any(), any(), any(), any())(any(), any())) thenReturn returnValue

    def mockRoute(url: Call) =
      when(router.getRouteNewAuth(any(), any(), any(), any())(any(), any())) thenReturn Future.successful(Redirect(url))

    def mockRoute =
      when(router.getRouteNewAuth(any(), any(), any(), any())(any(), any())) thenReturn defaultResult

    def verify(credId: String, pageId: PageId, model: T, edit: Boolean = false) =
      org.mockito.Mockito.verify(router).getRouteNewAuth(eqTo(credId), eqTo(pageId), eqTo(model), eqTo(edit))(any(), any())
  }
}