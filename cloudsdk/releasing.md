# Making official releases 

These are the steps for releasing an updated version of the Particle SDK.
For example, if you were releasing version `2.4.2`, you'd do the following:

1. Update the `version` field in `cloudsdk/build.gradle` to `'2.4.2'`
2. Search the README.md for instances of "`io.particle:cloudsdk`" and update the version 
field on each to `2.4.2`
3. Commit and push the previous two changes
4. Build a release and publish it to JCenter.  From the `cloudsdk` dir, do: `../gradlew clean build install bintrayUpload`
5. Tag the release: `git tag 2.4.2`
6. Push the tag: `git push origin 2.4.2`

