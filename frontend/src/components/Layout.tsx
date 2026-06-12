import { Outlet } from "react-router-dom";
import { Navbar } from "./Navbar";

export function Layout() {
  return (
    <div className="min-h-screen bg-white">
      <Navbar />
      <main className="mx-auto max-w-[1000px] px-4 py-5">
        <Outlet />
      </main>
      <footer className="mx-auto max-w-[1000px] px-4 py-6 text-[11px] text-gray-400">
        CodeArena — a Codeforces-style competitive programming platform.
      </footer>
    </div>
  );
}
