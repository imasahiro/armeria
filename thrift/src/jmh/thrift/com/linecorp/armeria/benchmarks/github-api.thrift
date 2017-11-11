/*
 * Copyright 2017 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

namespace java com.linecorp.armeria.benchmarks

struct Timestamp {
  1: i64 seconds;
  2: i32 nanos
}

struct Empty {
}

enum Type {
    Unknown = 0;
    User = 1;
    Organization = 2;
}

struct User {
    1: string login,
    2: i32 id,
    3: string avatar_url,
    4: string gravatar_id,
    5: string url,
    6: string html_url,
    7: string followers_url,
    8: string following_url,
    9: string gists_url,
    11: string starred_url,
    12: string subscriptions_url,
    13: string organizations_url,
    14: string repos_url,
    15: string events_url,
    16: string received_events_url,
    17: Type type,
    18: bool site_admin,
}

struct Item {
    1: i32 id,
    2: string name,
    3: string full_name,
    4: User owner,
    5: bool is_private,
    6: string html_url,
    7: string description,
    8: bool fork,
    9: string url,
    10: string forks_url,
    11: string keys_url,
    12: string collaborators_url,
    13: string teams_url,
    14: string hooks_url,
    15: string issue_events_url,
    16: string events_url,
    17: string assignees_url,
    18: string branches_url,
    19: string tags_url,
    20: string blobs_url,
    21: string git_tags_url,
    22: string git_refs_url,
    23: string trees_url,
    24: string statuses_url,
    25: string languages_url,
    26: string stargazers_url,
    27: string contributors_url,
    28: string subscribers_url,
    29: string subscription_url,
    30: string commits_url,
    31: string git_commits_url,
    32: string comments_url,
    33: string issue_comment_url,
    34: string contents_url,
    35: string compare_url,
    36: string merges_url,
    37: string archive_url,
    38: string downloads_url,
    39: string issues_url,
    40: string pulls_url,
    41: string milestones_url,
    42: string notifications_url,
    43: string labels_url,
    44: string releases_url,
    45: string deployments_url,
    46: Timestamp created_at,
    47: Timestamp updated_at,
    48: Timestamp pushed_at,
    49: string git_url,
    50: string ssh_url,
    51: string clone_url,
    52: string svn_url,
    53: string homepage,
    54: i32 size,
    55: i32 stargazers_count,
    56: i32 watchers_count,
    // Note: Maybe this is an enum.
    57: string language,
    58: bool has_issues,
    59: bool has_projects,
    60: bool has_downloads,
    61: bool has_wiki,
    62: bool has_pages,
    63: i32 forks_count,
    64: string mirror_url,
    65: i32 open_issues_count,
    66: i32 forks,
    67: i32 open_issues,
    68: i32 watchers,
    69: string default_branch,
    70: double score,
}

struct SearchResponse {
    1: i32 total_count,
    2: bool incomplete_results,
    3: list<Item> items,
}

service GithubService {
    SearchResponse simple(1: SearchResponse req);
    Empty empty(1: Empty req);
}
