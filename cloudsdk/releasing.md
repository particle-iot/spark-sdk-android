# Making official releases 

These are the steps for releasing an updated version of the Particle SDK.
For example, if you were releasing version `2.4.2`, you'd do the following:

1. Pull from origin to ensure you have the latest upstream changes
2. Update the `version` field in `cloudsdk/build.gradle` to `'2.4.2'`
3. Search the README.md for instances of "`io.particle:cloudsdk`" and update the version 
field on each to `2.4.2`
4. Build a release and publish it to JCenter.  From the `cloudsdk` dir, 
do: `../gradlew clean build install bintrayUpload`
5. Update the example app to pull the new version from JCenter, clean its build, and 
then build & run the example app as a final smoke test.
6. Commit and push the previous two changes
7. Tag the release: `git tag v2.4.2`  (note the "v" at the beginning)
8. Push the tag: `git push origin v2.4.2`  (again, note the "v")
9. (if applicable) announce the update via the appropriate channels
