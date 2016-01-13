# Making official releases 

These are the steps for releasing an updated version of the Particle SDK.
For example, if you were releasing version `2.4.2`, you'd do the following:

1. Make sure the CHANGELOG is current
2. Pull from origin to ensure you have the latest upstream changes
3. Update the `version` field in `cloudsdk/build.gradle` to `'2.4.2'`
4. Build a release and publish it to JCenter.  From the `cloudsdk` dir, 
do: `../gradlew clean build install bintrayUpload`
5. Submit a PR to the docs site updating the version code in `android.md` to `2.4.2`
6. Update the example app to pull the new version from JCenter, clean its build, and 
then build & run the example app as a final smoke test.
7. Commit and push the previous two changes
8. Tag the release: `git tag v2.4.2`  (note the "v" at the beginning)
9. Push the tag: `git push origin v2.4.2`  (again, note the "v")
10. (if applicable) announce the update via the appropriate channels
