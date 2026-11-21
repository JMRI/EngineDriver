"""Compare two Android lint XML reports and list the issues the first adds.

Usage: compare_lint.py SUMMARY_MD HEAD_XML HEAD_ROOT [BASE_XML BASE_ROOT]

HEAD_ROOT and BASE_ROOT are the checkout folders, so file paths can be compared
between the two. An issue is matched by its check, file and message, not its line,
so code that only moves does not count as new.

Appends a Markdown summary to SUMMARY_MD (the job summary in GitHub Actions), and
prints a GitHub Actions error annotation for each new issue, which GitHub shows on
that line in the pull request's changed files. Exits 1 if the head report has issues
the base report does not, 0 otherwise (and always 0 when no base report is given).
"""
import collections
import os
import sys
import xml.etree.ElementTree as ET


def read(path, root):
    """Return the issues in a lint XML report, with file paths relative to root."""
    issues = []
    root = os.path.abspath(root)
    for issue in ET.parse(path).getroot().iter("issue"):
        loc = issue.find("location")
        f = loc.get("file", "") if loc is not None else ""
        line = loc.get("line", "") if loc is not None else ""
        if os.path.isabs(f):
            f = os.path.relpath(f, root)
        issues.append({
            "severity": issue.get("severity", ""),
            "id": issue.get("id", ""),
            "file": f.replace("\\", "/"),
            "line": line,
            "message": issue.get("message", ""),
        })
    return issues


def key(issue):
    return (issue["id"], issue["file"], issue["message"])


def new_and_cleared(head, base):
    """Issues in head beyond those in base, and a count by check of base issues head no longer has."""
    remaining = collections.Counter(key(i) for i in base)
    new = []
    for i in head:
        k = key(i)
        if remaining[k] > 0:
            remaining[k] -= 1
        else:
            new.append(i)
    cleared = collections.Counter()
    for k, n in remaining.items():
        if n:
            cleared[k[0]] += n
    return new, cleared


def summary(head, base, new, cleared):
    """The Markdown summary, as a list of lines."""
    counts = ", ".join("%s %d" % kv for kv in sorted(collections.Counter(i["severity"] for i in head).items()))
    lines = ["## Android lint", ""]
    if base is None:
        lines.append("%d issues (%s)." % (len(head), counts or "none"))
        return lines
    lines += ["%d issues (%s); the base branch has %d." % (len(head), counts or "none", len(base)), ""]
    if cleared:
        lines += ["Cleared by this change: " + ", ".join("%s %d" % kv for kv in sorted(cleared.items())), ""]
    if not new:
        lines.append("No new lint issues compared with the base branch.")
        return lines
    lines += ["**%d new lint issue%s compared with the base branch:**" % (len(new), "" if len(new) == 1 else "s"),
              "",
              "| Check | Severity | Where | Message |",
              "|-------|----------|-------|---------|"]
    for i in new:
        where = "%s:%s" % (i["file"], i["line"]) if i["line"] else i["file"]
        lines.append("| %s | %s | `%s` | %s |" % (i["id"], i["severity"], where,
                                                  i["message"].replace("|", "\\|").replace("\n", " ")))
    return lines


def escape(text, prop=False):
    """Escape text for a GitHub Actions workflow command (prop: for a property value)."""
    text = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    return text.replace(":", "%3A").replace(",", "%2C") if prop else text


def annotation(issue):
    """A GitHub Actions error annotation for a new issue, shown on its line in the pull request."""
    where = "file=" + escape(issue["file"], True)
    if issue["line"]:
        where += ",line=" + issue["line"]
    return "::error %s,title=%s::%s" % (where, escape("New lint issue: " + issue["id"], True),
                                        escape(issue["message"]))


def main(argv):
    summary_path, head_xml, head_root = argv[0], argv[1], argv[2]
    head = read(head_xml, head_root)
    base = read(argv[3], argv[4]) if len(argv) >= 5 else None
    new, cleared = new_and_cleared(head, base) if base is not None else ([], collections.Counter())
    new.sort(key=lambda i: (i["file"], int(i["line"] or 0)))

    with open(summary_path, "a", encoding="utf-8") as out:
        out.write("\n".join(summary(head, base, new, cleared)) + "\n")
    for i in new:
        print(annotation(i))
    return 1 if new else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
