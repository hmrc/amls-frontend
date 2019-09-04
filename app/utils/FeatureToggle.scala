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

///*
// * Copyright 2019 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package utils
//
//import com.google.inject.Inject
//import config.CachedStaticHtmlPartialProvider
//import play.api.mvc._
//
//import scala.concurrent.Future
//
//class FeatureToggle @Inject()(feature: Boolean, implicit val partialProvider: CachedStaticHtmlPartialProvider) {
//  def apply(action: Action[AnyContent]): Action[AnyContent] =
//    if (feature) action else FeatureToggle.notFound
//}
//
//object FeatureToggle {
//  val notFound = Action.async {
//    implicit request =>
//      Future.successful(Results.NotFound(ControllerHelper.notFoundView(request, implicitly)))
//  }
//}
