package com.typesafe.sbt.web.incremental

import java.io.File
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import _root_.sbt.IO

@RunWith(classOf[JUnitRunner])
class IncrementalSpec extends Specification {

  "the runIncremental method" should {

    "always perform an op when there's no cache file" in {
      IO.withTemporaryDirectory { tmpDir =>
        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map[String,OpResult]("op1" -> OpFailure),
            prunedOps must_== List("op1")
          )
        }
      }
    }

    "rerun an op when it failed last time" in {
      IO.withTemporaryDirectory { tmpDir =>
        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpFailure),
            prunedOps must_== List("op1")
          )
        }
        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpFailure),
            prunedOps must_== List("op1")
          )
        }
      }
    }

    "skip an op if nothing's changed" in {
      IO.withTemporaryDirectory { tmpDir =>
        val file1 = new File(tmpDir, "1")
        IO.write(file1, "x")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpSuccess(filesRead = Set(file1), filesWritten = Set())),
            prunedOps must_== List("op1")
          )
        }
        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map.empty,
            prunedOps must_== List()
          )
        }
      }
    }

    "rerun an op when the file it read has changed" in {
      IO.withTemporaryDirectory { tmpDir =>
        val file1 = new File(tmpDir, "1")
        IO.write(file1, "x")
        val file2 = new File(tmpDir, "2")
        IO.write(file1, "x")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpSuccess(filesRead = Set(file1), filesWritten = Set(file2))),
            prunedOps must_== List("op1")
          )
        }

        IO.write(file1, "y")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map.empty,
            prunedOps must_== List("op1")
          )
        }
      }
    }

    "rerun an op when the file it wrote has changed" in {
      IO.withTemporaryDirectory { tmpDir =>
        val file1 = new File(tmpDir, "1")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          IO.write(file1, "x")
          (
            Map("op1" -> OpSuccess(filesRead = Set(), filesWritten = Set(file1))),
            prunedOps must_== List("op1")
          )
        }

        IO.write(file1, "y")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map.empty,
            prunedOps must_== List("op1")
          )
        }
      }
    }

    "rerun an op when the file it wrote has been deleted" in {
      IO.withTemporaryDirectory { tmpDir =>
        val file1 = new File(tmpDir, "1")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          IO.write(file1, "x")
          (
            Map("op1" -> OpSuccess(filesRead = Set(), filesWritten = Set(file1))),
            prunedOps must_== List("op1")
          )
        }

        IO.delete(file1)

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map.empty,
            prunedOps must_== List("op1")
          )
        }
      }
    }

    "rerun an op when some of the files it read have changed" in {
      IO.withTemporaryDirectory { tmpDir =>
        val file1 = new File(tmpDir, "1")
        val file2 = new File(tmpDir, "2")
        IO.write(file1, "x")
        IO.write(file2, "x")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpSuccess(filesRead = Set(file1, file2), filesWritten = Set())),
            prunedOps must_== List("op1")
          )
        }

        IO.write(file2, "y")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map.empty,
            prunedOps must_== List("op1")
          )
        }
      }
    }

    "rerun an op when some of the files it wrote have changed" in {
      IO.withTemporaryDirectory { tmpDir =>
        val file1 = new File(tmpDir, "1")
        val file2 = new File(tmpDir, "2")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          IO.write(file1, "x")
          IO.write(file2, "x")
          (
            Map("op1" -> OpSuccess(filesRead = Set(), filesWritten = Set(file1, file2))),
            prunedOps must_== List("op1")
          )
        }

        IO.write(file2, "y")

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map.empty,
            prunedOps must_== List("op1")
          )
        }
      }
    }

    "run multiple ops" in {
      IO.withTemporaryDirectory { tmpDir =>
        val file1 = new File(tmpDir, "1")
        val file2 = new File(tmpDir, "2")
        val file3 = new File(tmpDir, "3")
        val file4 = new File(tmpDir, "4")
        IO.write(file1, "x")
        IO.write(file2, "x")
        IO.write(file3, "x")
        IO.write(file4, "x")

        runIncremental(tmpDir, List("op1", "op2", "op3", "op4")) { prunedOps =>
          (
            Map(
              "op1" -> OpSuccess(filesRead = Set(file1), filesWritten = Set()),
              "op2" -> OpSuccess(filesRead = Set(file2), filesWritten = Set(file3)),
              "op3" -> OpSuccess(filesRead = Set(), filesWritten = Set(file4)),
              "op4" -> OpFailure
            ),
            prunedOps must_== List("op1", "op2", "op3", "op4")
          )
        }

        IO.write(file1, "y")
        IO.write(file4, "y")

        runIncremental(tmpDir, List("op1", "op2", "op3", "op4")) { prunedOps =>
          (
            Map(
              "op1" -> OpSuccess(filesRead = Set(file1), filesWritten = Set()),
              "op3" -> OpSuccess(filesRead = Set(), filesWritten = Set(file4)),
              "op4" -> OpFailure
            ),
            prunedOps must_== List("op1", "op3", "op4")
          )
        }
      }
    }

    "vacuum unneeded ops from the cache" in {
      IO.withTemporaryDirectory { tmpDir =>

        // Create an empty cache
        runIncremental(tmpDir, List[String]()) { prunedOps =>
          (
            Map.empty,
            prunedOps must_== List()
          )
        }

        val cacheFile = new File(tmpDir, "op-cache")
        val emptyCacheFileLength = cacheFile.length()

        val file1 = new File(tmpDir, "1")
        IO.write(file1, "x")

        // Run with a successful op that will be cached

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpSuccess(filesRead = Set(file1), filesWritten = Set())),
            prunedOps must_== List("op1")
          )
        }

        cacheFile.length() must_!= emptyCacheFileLength

        // Run with different set of ops - should vacuum old ops

        runIncremental(tmpDir, List("op9")) { prunedOps =>
          (
            Map("op9" -> OpFailure),
            prunedOps must_== List("op9")
          )
        }

        // Check cache file is empty again, i.e. op1 has been vacuumed

        cacheFile.length() must_== emptyCacheFileLength

      }
    }

    "rerun an op if its hash changes" in {
      IO.withTemporaryDirectory { tmpDir =>
        val file1 = new File(tmpDir, "1")
        val file2 = new File(tmpDir, "2")
        IO.write(file1, "x")
        IO.write(file2, "x")

        var hashPrefix = ""
        implicit val hasher = OpInputHasher[String](op => OpInputHash.hashString(hashPrefix + op))

        // Cache ops with an initial hash prefix

        hashPrefix = "1/"

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpSuccess(filesRead = Set(file1), filesWritten = Set(file2))),
            prunedOps must_== List("op1")
          )
        }

        // No ops should run because we leave the hash prefix the same

        hashPrefix = "1/"

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map(),
            prunedOps must_== List()
          )
        }

        // All ops should run again because we changed the hash prefix

        hashPrefix = "2/"

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpSuccess(filesRead = Set(file1), filesWritten = Set(file2))),
            prunedOps must_== List("op1")
          )
        }
      }
    }

    "rerun an op if the op hasher returns NoCache" in {
      IO.withTemporaryDirectory { tmpDir =>

        implicit val hasher = OpInputHasher.noCache[String]

        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpSuccess(filesRead = Set.empty, filesWritten = Set.empty)),
            prunedOps must_== List("op1")
            )
        }
        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map("op1" -> OpSuccess(filesRead = Set.empty, filesWritten = Set.empty)),
            prunedOps must_== List("op1")
            )
        }
      }
    }

    "fail when runOps gives result for unknown op" in {
      IO.withTemporaryDirectory { tmpDir =>
        runIncremental(tmpDir, List("op1")) { prunedOps =>
          (
            Map[String,OpResult]("op2" -> OpFailure),
            prunedOps must_== List("op1")
          )
        } must throwA[IllegalArgumentException]
      }
    }

  }

}