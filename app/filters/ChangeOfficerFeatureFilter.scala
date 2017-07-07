/*
 * Copyright 2017 HM Revenue & Customs
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

package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import play.api.mvc.Results._

import scala.concurrent.Future

class ChangeOfficerFeatureFilter @Inject()(config: ServicesConfig)(implicit val mat: Materializer) extends Filter {
  override def apply(nextAction: (RequestHeader) => Future[Result])(request: RequestHeader) = {
    if (controllers.changeofficer.Flow.journeyUrls.contains(request.path)) {
      if (config.getConfBool("feature-toggle.change-officer", false)) {
        nextAction(request)
      } else {
        Future.successful(NotFound)
      }
    } else {
      nextAction(request)
    }
  }
}
