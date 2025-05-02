$dependencies = @(
    @{ name = "core" },
    @{ name = "firebase" }
)

foreach ($dep in $dependencies) {
    Write-Host "Dependency: $($dep.name)"
}
