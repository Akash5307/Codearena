import { useLayoutEffect, useRef } from "react";

// A lightweight, dependency-free code editor: a monospace textarea with smart
// editing — auto-indentation, bracket/quote auto-closing, and indent-aware
// Tab/Backspace — plus a line-number gutter. Keeps the bundle lean and
// offline-safe; can be swapped for Monaco/CodeMirror later (see CLAUDE.md).

const INDENT = "    "; // 4 spaces — one indent level
const OPEN_TO_CLOSE: Record<string, string> = { "(": ")", "[": "]", "{": "}" };
const CLOSERS = new Set([")", "]", "}"]);
const QUOTES = new Set(['"', "'", "`"]);

export function CodeEditor({
  value,
  onChange,
  rows = 18,
}: {
  value: string;
  onChange: (v: string) => void;
  rows?: number;
}) {
  const ref = useRef<HTMLTextAreaElement>(null);
  const gutterRef = useRef<HTMLPreElement>(null);
  // Selection to apply after the controlled value re-renders. Set in a
  // layout effect so the caret lands correctly on the new DOM text.
  const pendingSel = useRef<[number, number] | null>(null);

  useLayoutEffect(() => {
    if (pendingSel.current && ref.current) {
      const [s, e] = pendingSel.current;
      ref.current.selectionStart = s;
      ref.current.selectionEnd = e;
      pendingSel.current = null;
    }
  });

  // Replace the text and queue the new caret/selection.
  function apply(next: string, selStart: number, selEnd: number = selStart) {
    pendingSel.current = [selStart, selEnd];
    onChange(next);
  }

  // Move the caret without changing the text (value unchanged → no re-render).
  function moveCaret(pos: number) {
    const ta = ref.current;
    if (ta) ta.selectionStart = ta.selectionEnd = pos;
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    const ta = e.currentTarget;
    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    const v = value;
    const prevChar = v[start - 1] ?? "";
    const nextChar = v[end] ?? "";

    // ---- Tab / Shift+Tab: indent or outdent ----
    if (e.key === "Tab") {
      e.preventDefault();
      const lineStart = v.lastIndexOf("\n", start - 1) + 1;
      const multiline = v.slice(start, end).includes("\n");

      if (!e.shiftKey && !multiline) {
        apply(v.slice(0, start) + INDENT + v.slice(end), start + INDENT.length);
        return;
      }

      const block = v.slice(lineStart, end);
      const lines = block.split("\n");
      if (e.shiftKey) {
        // outdent: strip up to one indent unit (4 spaces or a tab) per line
        let removedFirst = 0;
        let removedTotal = 0;
        const out = lines.map((l, i) => {
          const m = l.match(/^( {1,4}|\t)/);
          const r = m ? m[0].length : 0;
          if (i === 0) removedFirst = r;
          removedTotal += r;
          return l.slice(r);
        });
        apply(
          v.slice(0, lineStart) + out.join("\n") + v.slice(end),
          Math.max(lineStart, start - removedFirst),
          end - removedTotal,
        );
      } else {
        // indent every selected line
        const out = lines.map((l) => INDENT + l);
        apply(
          v.slice(0, lineStart) + out.join("\n") + v.slice(end),
          start + INDENT.length,
          end + INDENT.length * lines.length,
        );
      }
      return;
    }

    // ---- Enter: keep indentation, add a level after an opener / ":" ----
    if (e.key === "Enter") {
      e.preventDefault();
      const lineStart = v.lastIndexOf("\n", start - 1) + 1;
      const curLine = v.slice(lineStart, start);
      const indent = (curLine.match(/^[ \t]*/) ?? [""])[0];

      const pairExpand =
        (prevChar === "{" && nextChar === "}") ||
        (prevChar === "[" && nextChar === "]") ||
        (prevChar === "(" && nextChar === ")");
      const opensBlock = /[{[(]$/.test(prevChar) || /:\s*$/.test(curLine);

      if (pairExpand) {
        // { | }  ->  {\n    |\n}
        const inner = "\n" + indent + INDENT;
        const outer = "\n" + indent;
        apply(v.slice(0, start) + inner + outer + v.slice(end), start + inner.length);
      } else if (opensBlock) {
        const insert = "\n" + indent + INDENT;
        apply(v.slice(0, start) + insert + v.slice(end), start + insert.length);
      } else {
        const insert = "\n" + indent;
        apply(v.slice(0, start) + insert + v.slice(end), start + insert.length);
      }
      return;
    }

    // ---- Auto-close brackets (and wrap a selection) ----
    if (OPEN_TO_CLOSE[e.key]) {
      e.preventDefault();
      const close = OPEN_TO_CLOSE[e.key];
      if (start !== end) {
        const sel = v.slice(start, end);
        apply(v.slice(0, start) + e.key + sel + close + v.slice(end), start + 1, end + 1);
      } else {
        apply(v.slice(0, start) + e.key + close + v.slice(end), start + 1);
      }
      return;
    }

    // ---- Type a closer right before the auto-inserted one: just step over it ----
    if (CLOSERS.has(e.key) && start === end && nextChar === e.key) {
      e.preventDefault();
      moveCaret(start + 1);
      return;
    }

    // ---- Quotes: auto-close, wrap selection, or step over ----
    if (QUOTES.has(e.key)) {
      if (start === end && nextChar === e.key) {
        e.preventDefault();
        moveCaret(start + 1);
        return;
      }
      e.preventDefault();
      if (start !== end) {
        const sel = v.slice(start, end);
        apply(v.slice(0, start) + e.key + sel + e.key + v.slice(end), start + 1, end + 1);
      } else {
        apply(v.slice(0, start) + e.key + e.key + v.slice(end), start + 1);
      }
      return;
    }

    // ---- Backspace: delete an empty pair, or a whole indent unit ----
    if (e.key === "Backspace" && start === end && start > 0) {
      const emptyPair =
        (OPEN_TO_CLOSE[prevChar] && OPEN_TO_CLOSE[prevChar] === nextChar) ||
        (QUOTES.has(prevChar) && nextChar === prevChar);
      if (emptyPair) {
        e.preventDefault();
        apply(v.slice(0, start - 1) + v.slice(start + 1), start - 1);
        return;
      }
      const lineStart = v.lastIndexOf("\n", start - 1) + 1;
      const before = v.slice(lineStart, start);
      if (before.length > 0 && /^ +$/.test(before)) {
        const remove = before.length % INDENT.length === 0 ? INDENT.length : before.length % INDENT.length;
        e.preventDefault();
        apply(v.slice(0, start - remove) + v.slice(start), start - remove);
        return;
      }
    }
  }

  const lineCount = Math.max(value.split("\n").length, rows);

  return (
    <div className="flex border border-cf-border bg-white font-mono text-[13px]">
      <pre
        ref={gutterRef}
        aria-hidden
        className="select-none overflow-hidden bg-cf-panel px-2 py-2 text-right leading-[1.5] text-gray-400"
      >
        {Array.from({ length: lineCount }, (_, i) => i + 1).join("\n")}
      </pre>
      <textarea
        ref={ref}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        onScroll={(e) => {
          // keep the line-number gutter aligned while scrolling
          if (gutterRef.current) gutterRef.current.scrollTop = e.currentTarget.scrollTop;
        }}
        spellCheck={false}
        rows={rows}
        wrap="off"
        className="flex-1 resize-y overflow-auto px-2 py-2 leading-[1.5] outline-none"
        placeholder="// write your solution here"
      />
    </div>
  );
}
