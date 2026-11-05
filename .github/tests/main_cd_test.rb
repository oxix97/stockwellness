require 'minitest/autorun'
require 'yaml'
require 'tmpdir'
require 'open3'
require 'json'

class MainCdTest < Minitest::Test
  def workflow(path)
    YAML.load_file(File.join('.github/workflows', path))
  end

  def test_main_push_runs_ci
    assert_includes workflow('ci.yml')[true]['push']['branches'], 'main'
  end

  def test_deployment_routes_successful_main_push_ci_and_preserves_tag_releases
    %w[api batch].each do |service|
      doc = workflow("deploy-#{service}.yaml")
      trigger = doc[true]
      assert trigger.key?('workflow_call'), 'workflow_call trigger is missing'
      caller = workflow('ci.yml')['jobs']["deploy-#{service}"]
      assert_equal 'build', caller['needs']
      assert_includes caller['if'], "refs/heads/main"
      assert_includes caller['if'], "push"
      assert_equal "./.github/workflows/deploy-#{service}.yaml", caller['uses']
      assert_includes trigger['push']['tags'], "#{service}-v*.*.*"
      assert doc['concurrency'], "#{service} concurrency missing"
      assert_equal "stockwellness-#{service}-production", doc['concurrency']['group']
      assert_equal false, doc['concurrency']['cancel-in-progress']
      guard = doc['jobs']['verify-ci']['if']
      %w[tag push main].each { |word| assert_includes guard, word }
      assert_includes doc['env']['DEPLOY_SHA'], 'github.sha'
      assert_includes doc['env']['DEPLOY_TAG'], 'github.sha'
      verify = doc['jobs']['verify-ci']['steps'].map { |s| s['run'] }.compact.join("\n")
      assert_includes verify, 'ref/heads/main'
      assert_includes verify, 'DEPLOY_SHA'
      assert_includes verify, 'select(.event != "pull_request")'
      assert_includes verify, 'actions/runs/'
      assert_includes verify, 'Build & Test'
      build = doc['jobs']["build-#{service}"]['steps']
      assert_equal '${{ env.DEPLOY_SHA }}', build.find { |s| s['uses'] == 'actions/checkout@v4' }['with']['ref']
      meta = build.find { |s| s['id'] == 'meta' }['with']['tags']
      assert_includes meta, '${{ env.DEPLOY_TAG }}'
      deploy = doc['jobs']["deploy-#{service}"]['steps'].map { |s| s['run'] }.compact.join("\n")
      assert_includes deploy, '${{ env.DEPLOY_TAG }}'
      assert_includes deploy, '${{ env.DEPLOY_SHA }}'
      assert_includes deploy, 'ref/heads/main'
    end
  end
  def test_verification_script_rejects_stale_sha_and_unsuccessful_or_pr_ci
    sha = 'a' * 40
    good = { workflow_runs: [{ id: 42, head_sha: sha, event: 'push', status: 'in_progress', conclusion: nil }] }
    Dir.mktmpdir do |dir|
      fake = File.join(dir, 'gh')
      File.write(fake, "#!/bin/bash\ncase \"$1\" in api) case \"$2\" in */git/ref/heads/main) echo \"$GH_FIXTURE_MAIN\";; */actions/runs/*/jobs*) printf '%s' \"$GH_FIXTURE_JOBS\";; *) printf '%s' \"$GH_FIXTURE_RUNS\";; esac;; esac\n")
      File.chmod(0o755, fake)
      %w[api batch].each do |service|
        script = workflow("deploy-#{service}.yaml")['jobs']['verify-ci']['steps'][0]['run']
        run = lambda do |event, current, results, job_status = 'success'|
          env = { 'PATH' => "#{dir}:#{ENV.fetch('PATH')}", 'GITHUB_REF' => event == 'workflow_run' ? 'refs/heads/main' : 'refs/tags/v1.0.0',
                  'REPOSITORY' => 'owner/repo', 'DEPLOY_SHA' => sha,
                  'GH_FIXTURE_MAIN' => current, 'GH_FIXTURE_RUNS' => JSON.generate(results),
                  'GH_FIXTURE_JOBS' => JSON.generate({ jobs: [{name: 'Build & Test', status: 'completed', conclusion: job_status}] }) }
          Open3.capture3(env, 'bash', '-eo', 'pipefail', '-c', script)[2].success?
        end
        assert run.call('workflow_run', sha, good), "#{service} rejects successful main CI"
        refute run.call('workflow_run', 'b' * 40, good), "#{service} allows stale main"
        assert run.call('push', 'b' * 40, good), "#{service} rejects valid tag"
        refute run.call('push', sha, { workflow_runs: [] }), "#{service} accepts absent CI"
        wrong_sha = Marshal.load(Marshal.dump(good))
        wrong_sha[:workflow_runs][0][:head_sha] = 'b' * 40
        refute run.call('push', sha, wrong_sha), "#{service} accepts another commit's CI"
        second_guard = workflow("deploy-#{service}.yaml")['jobs']["deploy-#{service}"]['steps'].find { |step| step['name'] == 'Reject superseded main deployment' }['run']
        guard_env = { 'PATH' => "#{dir}:#{ENV.fetch('PATH')}", 'REPOSITORY' => 'owner/repo', 'DEPLOY_SHA' => sha, 'GH_FIXTURE_MAIN' => 'b' * 40 }
        refute Open3.capture3(guard_env, 'bash', '-eo', 'pipefail', '-c', second_guard)[2].success?, "#{service} allows a stale webhook"
        %w[failure cancelled].each { |status| refute run.call('push', sha, good, status), "#{service} accepts #{status} build" }
        %w[pull_request].each do |bad|
          fixture = Marshal.load(Marshal.dump(good))
          if bad == 'pull_request'
            fixture[:workflow_runs][0][:event] = bad
          else
            fixture[:workflow_runs][0][:conclusion] = bad
          end
          refute run.call('push', sha, fixture), "#{service} accepts #{bad} CI"
        end
      end
    end
  end

end
