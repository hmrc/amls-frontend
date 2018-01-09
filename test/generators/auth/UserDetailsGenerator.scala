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

package generators.auth

import generators.BaseGenerator
import models.auth.{CredentialRole, UserDetails}
import org.scalacheck.Gen

//noinspection ScalaStyle
trait UserDetailsGenerator extends BaseGenerator {

  val userDetailsGen: Gen[UserDetails] = for {
    name <- stringOfLengthGen(10)
    group <- stringOfLengthGen(20)
    credentialRole <- Gen.oneOf(CredentialRole.User, CredentialRole.Assistant)
    groupId <- numSequence(9)
  } yield UserDetails(name, None, group, Some(credentialRole), Some(groupId))

}
