# eff

[![Build Status](https://travis-ci.org/atnos-org/eff-scalaz.png?branch=master)](https://travis-ci.org/atnos-org/eff-scalaz)
[![Join the chat at https://gitter.im/atnos-org/eff](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/atnos-org/eff?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Extensible effects are an alternative to monad transformers for computing with effects in a functional way.
This library is based on the "free-er" monad and an "open union" of effects described in
Oleg Kiselyov in [Freer monads, more extensible effects](http://okmij.org/ftp/Haskell/extensible/more.pdf).

You can learn more in the User Guide:

 - [your first effects](http://atnos-org.github.io/eff-scalaz/org.atnos.site.Introduction.html)
 - [included effects: `Reader`, `Writer`, `Eval`, `State`,...](http://atnos-org.github.io/eff-scalaz/org.atnos.site.OutOfTheBox.html)
 - [how to use implicits to get type-inference right](http://atnos-org.github.io/eff-scalaz/org.atnos.site.Implicits.html)
 - [using an open or a closed union of effects](http://atnos-org.github.io/eff-scalaz/org.atnos.site.OpenClosed.html)
 - [create your own effects](http://atnos-org.github.io/eff-scalaz/org.atnos.site.CreateEffects.html)
 - [use Member implicits](http://atnos-org.github.io/eff-scalaz/org.atnos.site.Implicits.html)
 - [working with different effect stacks](http://atnos-org.github.io/eff-scalaz/org.atnos.site.TransformStack.html)

You can also check out [this presentation](http://bit.ly/eff_flatmap_2016) at flatMap Oslo 2016 ([slides](http://www.slideshare.net/etorreborre/the-eff-monad-one-monad-to-rule-them-all)).

## Installation

You add `eff-scalaz` as an sbt dependency:
```scala
libraryDependencies += "org.atnos" %% "eff-scalaz" % "1.7.1"

// to write types like Reader[String, ?]
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")

// to get types like Reader[String, ?] (with more than one type parameter) correctly inferred
addCompilerPlugin("com.milessabin" % "si2712fix-plugin_2.11.8" % "1.2.0")
```

If you want to use the `scalaz.concurrent.Future` or `scalaz.concurrent.Task` effects:
```scala
libraryDependencies += "org.atnos" %% "eff-scalaz-concurrent" % "1.7.1"
```

# Contributing

[eff-scalaz](https://github.com/atnos-org/eff-scalaz/) is a [Typelevel](http://typelevel.org) project. This means we embrace pure, typeful, functional programming,
and provide a safe and friendly environment for teaching, learning, and contributing as described in the [Typelevel Code of Conduct](http://typelevel.org/conduct.html).

Feel free to open an issue if you notice a bug, have an idea for a feature, or have a question about the code. Pull requests are also gladly accepted.
