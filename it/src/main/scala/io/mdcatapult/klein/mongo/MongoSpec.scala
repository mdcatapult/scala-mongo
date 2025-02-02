/*
 * Copyright 2024 Medicines Discovery Catapult
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

package io.mdcatapult.klein.mongo

import java.time.LocalDateTime.now
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit.MILLIS
import java.util.UUID.randomUUID

import com.typesafe.config.{Config, ConfigFactory}
import io.mdcatapult.klein.mongo.ResultConverters.toInsertionResult
import org.bson.UuidRepresentation
import org.bson.codecs.UuidCodec
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters.{equal => Mequal}
import org.scalatest.OptionValues._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global

class MongoSpec extends AnyFlatSpec with Matchers with ScalaFutures {

  implicit val c: Config = ConfigFactory.load()
  implicit val r: CodecRegistry = fromRegistries(
    fromProviders(classOf[TestDoc]),
    fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
    DEFAULT_CODEC_REGISTRY
  )

  val mongo: Mongo = new Mongo()

  "Mongo" should "write test document when configured" in {

    implicit val collection: MongoCollection[TestDoc] =
      mongo.getCollection[TestDoc](
        "test",
        "test-coll"
      )

    val doc = TestDoc(randomUUID(), now(UTC))

    val written = collection.insertOne(doc).toFuture().map(toInsertionResult)
    val read = written.flatMap(_ => collection.find(Mequal("_id", doc._id)).toFuture())

    whenReady(written, Timeout(Span(10, Seconds))) { r =>
      r.acknowledged should be (true)
      r.insertedCount should be (1L)
    }

    whenReady(read, Timeout(Span(10, Seconds))) { docs =>
      val storedDoc = docs.headOption.value

      storedDoc._id should be(doc._id)
      storedDoc.created.truncatedTo(MILLIS) should be (doc.created.truncatedTo(MILLIS))
    }
  }

  "Mongo" should "be healthy" in {
    whenReady(mongo.checkHealth(), Timeout(Span(10, Seconds))) { result =>
      result should be (true)
    }
  }
  "Mongo" should "be using primary" in {
      val doc = mongo.readPreference.toDocument
      doc.getString("mode").asString().getValue should be ("primaryPreferred")
  }
}
