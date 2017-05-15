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

package utils

import jto.validation._
import jto.validation.ValidationError
import play.api.libs.json.{PathNode => _, _}
import cats.data.Validated.{Invalid, Valid}

trait JsonMapping {

  import play.api.libs.json
  import play.api.libs.json.{JsPath, JsValue, Reads, Writes, JsSuccess, JsError}
  import jto.validation.{KeyPathNode, IdxPathNode, PathNode}

  def nodeToJsNode(n: PathNode): json.PathNode = {
    n match {
      case KeyPathNode(key) =>
        json.KeyPathNode(key)
      case IdxPathNode(idx) =>
        json.IdxPathNode(idx)
    }
  }

  private def pathToJsPath(p: Path): JsPath =
    JsPath(p.path.map(nodeToJsNode _))

  def convertError(error: ValidationError): play.api.data.validation.ValidationError = {
    play.api.data.validation.ValidationError(error.message, error.args)
  }

  implicit def convertValidationErros(errors: Seq[ValidationError]): Seq[play.api.data.validation.ValidationError] = {
   errors.map(convertError(_))
  }

  implicit def errorConversion(errs: Seq[(Path, Seq[ValidationError])]): Seq[(JsPath, Seq[play.api.data.validation.ValidationError])] =
    errs map {
      case (path, errors) =>
        (pathToJsPath(path), convertValidationErros(errors))
    }

  implicit def genericJsonR[A]
  (implicit
   rule: Rule[JsValue, A]
  ): Reads[A] =
    Reads {
      json =>
        rule.validate(json) match {
          case Valid(x) => JsSuccess(x)
          case Invalid(error) => JsError(error)
        }
    }

  implicit def genericJsonW[A]
  (implicit
   write: Write[A, JsValue]
  ): Writes[A] =
    Writes {
      a =>
        write.writes(a)
    }

  // This is here to prevent NoSuchMethodErrors from the validation library
  implicit def pickInJson[II <: JsValue, O](p: Path)(implicit r: RuleLike[JsValue, O]): Rule[II, O] = {

    def search(path: Path, json: JsValue): Option[JsValue] = path.path match {
      case KeyPathNode(k) :: t =>
        json match {
          case JsObject(js) =>
            js.find(_._1 == k).flatMap(kv => search(Path(t), kv._2))
          case _ => None
        }
      case IdxPathNode(i) :: t =>
        json match {
          case JsArray(js) => js.lift(i).flatMap(j => search(Path(t), j))
          case _ => None
        }
      case Nil => Some(json)
    }

    Rule[II, JsValue] { json =>
      search(p, json) match {
        case None => Invalid(Seq(Path -> Seq(ValidationError("error.required"))))
        case Some(js) => Valid(js)
      }
    }.andThen(r)
  }
}

object JsonMapping extends JsonMapping
